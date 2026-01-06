package com.example.budgetquest.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseBackupManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val dbName = "budget_database"

    // 取得當前使用者狀態
    fun getCurrentUser() = auth.currentUser

    // 匿名登入 (保留原本功能)
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

    // [新增] Google 登入
    // 接收 Google Sign-In 傳回來的 idToken，並與 Firebase 驗證
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("登入失敗：無法取得使用者資訊"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseBackup", "Google Sign in failed", e)
            Result.failure(e)
        }
    }

    // [新增] 取得上次備份時間
    suspend fun getLastBackupTime(): Result<Long?> {
        val user = auth.currentUser ?: return Result.failure(Exception("尚未登入"))
        val storageRef = storage.reference.child("users/${user.uid}/backup.db")

        return try {
            val metadata = storageRef.metadata.await()
            // 回傳建立時間 (毫秒)
            Result.success(metadata.creationTimeMillis)
        } catch (e: Exception) {
            // 如果檔案不存在 (從未備份過)，這也是一種正常的結果，回傳 null
            // 判斷是否為 Object Not Found 錯誤
            if (e.message?.contains("Object does not exist") == true || (e as? com.google.firebase.storage.StorageException)?.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    // 上傳備份
    suspend fun uploadBackup(): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("尚未登入"))
        val dbFile = context.getDatabasePath(dbName)

        if (!dbFile.exists()) {
            return Result.failure(Exception("找不到本地資料庫檔案"))
        }

        val storageRef = storage.reference.child("users/${user.uid}/backup.db")

        return try {
            storageRef.putFile(Uri.fromFile(dbFile)).await()
            Result.success("上傳成功")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 下載並還原備份
    suspend fun restoreBackup(): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("尚未登入"))
        val storageRef = storage.reference.child("users/${user.uid}/backup.db")
        val dbFile = context.getDatabasePath(dbName)

        val tempFile = File(context.cacheDir, "temp_restore.db")

        return try {
            storageRef.getFile(tempFile).await()

            if (tempFile.exists()) {
                com.example.budgetquest.data.BudgetDatabase.getDatabase(context).close()
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()
                Result.success("還原成功")
            } else {
                Result.failure(Exception("下載失敗"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}