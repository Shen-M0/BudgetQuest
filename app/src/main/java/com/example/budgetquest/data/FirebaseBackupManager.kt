package com.example.budgetquest.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.budgetquest.R // [新增] 引用 R 資源
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class FirebaseBackupManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val dbName = "budget_database"

    fun getCurrentUser() = auth.currentUser

    suspend fun signInAnonymously(): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            true
        } catch (e: Exception) {
            Log.e("FirebaseBackup", "Anon Sign in failed", e)
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            // [提取] 登入失敗字串
            if (user != null) Result.success(user) else Result.failure(Exception(context.getString(R.string.error_login_failed_general)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastBackupTime(): Result<Long?> {
        // [提取] 尚未登入字串
        val user = auth.currentUser ?: return Result.failure(Exception(context.getString(R.string.error_not_logged_in)))
        val storageRef = storage.reference.child("users/${user.uid}/backup.db")
        return try {
            val metadata = storageRef.metadata.await()
            Result.success(metadata.creationTimeMillis)
        } catch (e: Exception) {
            if (e.message?.contains("Object does not exist") == true || (e as? StorageException)?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    // [完整實作] 上傳備份：資料庫 + 圖片
    suspend fun uploadBackup(): Result<String> = withContext(Dispatchers.IO) {
        // [提取] 尚未登入字串
        val user = auth.currentUser ?: return@withContext Result.failure(Exception(context.getString(R.string.error_not_logged_in)))
        val dbFile = context.getDatabasePath(dbName)

        // 1. 強制 Checkpoint (合併 WAL 資料)
        try {
            val db = BudgetDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (e: Exception) {
            Log.e("FirebaseBackup", "Checkpoint failed", e)
        }

        // [提取] 找不到檔案字串
        if (!dbFile.exists()) return@withContext Result.failure(Exception(context.getString(R.string.error_db_file_not_found)))

        try {
            // 2. 上傳資料庫
            val dbRef = storage.reference.child("users/${user.uid}/backup.db")
            dbRef.putFile(Uri.fromFile(dbFile)).await()

            // 3. 上傳圖片 (平行處理)
            uploadImages(user.uid)

            // [提取] 上傳成功字串
            Result.success(context.getString(R.string.msg_upload_success_simple))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadImages(uid: String) = coroutineScope {
        val dao = BudgetDatabase.getDatabase(context).budgetDao()
        // 抓取所有有圖片的紀錄
        val allExpenses = dao.getExpensesListByDate(0, Long.MAX_VALUE)

        val imageFiles = allExpenses
            .mapNotNull { it.imageUri }
            .filter { it.isNotBlank() }
            .map { File(it) }
            .filter { it.exists() } // 確保檔案還在
            .distinctBy { it.name } // 避免重複上傳

        // 平行上傳
        imageFiles.map { file ->
            async {
                try {
                    val ref = storage.reference.child("users/$uid/images/${file.name}")
                    ref.putFile(Uri.fromFile(file)).await()
                } catch (e: Exception) {
                    Log.e("FirebaseBackup", "Image upload failed: ${file.name}", e)
                }
            }
        }.awaitAll()
    }

    // [完整實作] 還原備份：資料庫 + 圖片 + 路徑修復
    suspend fun restoreBackup(): Result<String> = withContext(Dispatchers.IO) {
        // [提取] 尚未登入字串
        val user = auth.currentUser ?: return@withContext Result.failure(Exception(context.getString(R.string.error_not_logged_in)))
        val dbRef = storage.reference.child("users/${user.uid}/backup.db")
        val dbFile = context.getDatabasePath(dbName)

        // 暫存檔路徑 (WAL/SHM)
        val walFile = context.getDatabasePath("$dbName-wal")
        val shmFile = context.getDatabasePath("$dbName-shm")
        val tempFile = File(context.cacheDir, "temp_restore.db")

        try {
            // 1. 下載資料庫
            dbRef.getFile(tempFile).await()

            if (tempFile.exists()) {
                // 關閉目前連線
                BudgetDatabase.getDatabase(context).close()

                // 清除暫存檔 (關鍵! 避免讀到舊的緩存資料)
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                // 覆蓋主檔
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()

                // 2. 下載圖片
                restoreImages(user.uid)

                // 3. 修復資料庫路徑 (關鍵! 換手機後路徑會變)
                fixDatabaseImagePaths()

                // [提取] 還原成功字串
                Result.success(context.getString(R.string.msg_restore_success_simple))
            } else {
                // [提取] 下載失敗字串
                Result.failure(Exception(context.getString(R.string.error_download_failed)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreImages(uid: String) = coroutineScope {
        try {
            val imagesRef = storage.reference.child("users/$uid/images")
            val listResult = imagesRef.listAll().await()
            val localDir = context.filesDir // 存到 app 內部空間

            listResult.items.map { itemRef ->
                async {
                    try {
                        val localFile = File(localDir, itemRef.name)
                        itemRef.getFile(localFile).await()
                    } catch (e: Exception) {
                        Log.e("FirebaseBackup", "Image restore failed: ${itemRef.name}", e)
                    }
                }
            }.awaitAll()
        } catch (e: Exception) {
            Log.e("FirebaseBackup", "Failed to list images", e)
        }
    }

    // 修復資料庫中的圖片路徑
    private suspend fun fixDatabaseImagePaths() {
        val dao = BudgetDatabase.getDatabase(context).budgetDao()
        val allExpenses = dao.getExpensesListByDate(0, Long.MAX_VALUE)
        val localDir = context.filesDir.absolutePath

        allExpenses.forEach { expense ->
            val oldPath = expense.imageUri
            if (!oldPath.isNullOrBlank()) {
                val fileName = File(oldPath).name
                val newPath = "$localDir/$fileName"

                // 如果路徑不一致，就更新資料庫
                if (oldPath != newPath) {
                    dao.updateExpense(expense.copy(imageUri = newPath))
                }
            }
        }
    }
}