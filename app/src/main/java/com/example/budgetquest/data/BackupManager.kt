package com.example.budgetquest.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "budget_database"

    // [修改] 備份：將資料庫與圖片打包成 ZIP
    suspend fun backupDatabase(uri: Uri, budgetDao: BudgetDao) {
        withContext(Dispatchers.IO) {
            // 1. 強制寫入 (Checkpoint)
            try {
                budgetDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
            } catch (e: Exception) {
                Log.e("BackupManager", "Checkpoint failed", e)
            }

            val dbFile = context.getDatabasePath(dbName)
            val imagesDir = context.filesDir // 圖片存放目錄

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                    // 2. 壓縮資料庫檔案
                    if (dbFile.exists()) {
                        addToZip(zipOut, dbFile, dbFile.name)
                    }

                    // 3. 壓縮所有圖片檔案
                    // 這裡假設您的圖片都直接存在 filesDir 根目錄下 (ImageUtils 的邏輯)
                    val files = imagesDir.listFiles()
                    files?.forEach { file ->
                        // 只備份圖片檔 (根據您的命名規則 IMG_... 或其他特徵，或是全部備份)
                        if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png"))) {
                            addToZip(zipOut, file, "images/${file.name}")
                        }
                    }
                }
            }
        }
    }

    private fun addToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { input ->
            val entry = ZipEntry(entryName)
            zipOut.putNextEntry(entry)
            input.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }

    // [修改] 還原：解壓縮 ZIP 並恢復檔案
    suspend fun restoreDatabase(uri: Uri) {
        withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath(dbName)
            val imagesDir = context.filesDir

            // 確保目標目錄存在
            if (!imagesDir.exists()) imagesDir.mkdirs()

            // 1. 準備清除舊的 WAL/SHM 檔
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val entryName = entry.name

                        if (entryName == dbName) {
                            // 2. 還原資料庫主檔
                            // 先關閉資料庫連線 (這一步很重要)
                            com.example.budgetquest.data.BudgetDatabase.getDatabase(context).close()

                            // 刪除暫存檔以防衝突
                            if (walFile.exists()) walFile.delete()
                            if (shmFile.exists()) shmFile.delete()

                            FileOutputStream(dbFile).use { output ->
                                zipIn.copyTo(output)
                            }
                        } else if (entryName.startsWith("images/")) {
                            // 3. 還原圖片
                            // 去掉 "images/" 前綴取得檔名
                            val fileName = entryName.removePrefix("images/")
                            if (fileName.isNotEmpty()) {
                                val targetFile = File(imagesDir, fileName)
                                FileOutputStream(targetFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                        }

                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // 4. [關鍵] 修復資料庫內的圖片路徑
            // 因為還原到新手機後，絕對路徑前綴 (/data/user/0/...) 會改變
            fixDatabaseImagePaths(context)
        }
    }

    // 修復路徑的輔助函式
    private suspend fun fixDatabaseImagePaths(context: Context) {
        val dao = BudgetDatabase.getDatabase(context).budgetDao()
        // 取得所有消費紀錄
        val allExpenses = dao.getExpensesListByDate(0, Long.MAX_VALUE)
        val localDir = context.filesDir.absolutePath

        allExpenses.forEach { expense ->
            val oldPath = expense.imageUri
            if (!oldPath.isNullOrBlank()) {
                val fileName = File(oldPath).name
                // 組合出正確的新路徑
                val newPath = "$localDir/$fileName"

                // 如果路徑字串不一樣，就更新資料庫
                if (oldPath != newPath) {
                    dao.updateExpense(expense.copy(imageUri = newPath))
                }
            }
        }
    }
}