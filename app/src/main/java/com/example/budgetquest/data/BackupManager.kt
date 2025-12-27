package com.example.budgetquest.data

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "budget_database"

    // 備份資料庫
    suspend fun backupDatabase(uri: Uri, budgetDao: BudgetDao) {
        withContext(Dispatchers.IO) {
            // 1. 強制寫入 (Checkpoint)，把 WAL 暫存檔合併進 .db
            budgetDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))

            // 2. 取得資料庫檔案路徑
            val dbFile = context.getDatabasePath(dbName)

            // 3. 複製到使用者選擇的 Uri (Google Drive)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }
        }
    }

    // 還原資料庫
    suspend fun restoreDatabase(uri: Uri) {
        withContext(Dispatchers.IO) {
            // 1. 取得資料庫檔案路徑
            val dbFile = context.getDatabasePath(dbName)

            // 2. 關閉當前資料庫連接 (雖然 Room 會自動重連，但為了安全建議重啟 App)
            // 由於我們沒辦法直接關閉 Room Instance，我們直接覆蓋檔案
            // 為了避免 WAL 干擾，我們最好刪除舊的 WAL 檔
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 3. 將選擇的檔案覆蓋到本地資料庫
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}