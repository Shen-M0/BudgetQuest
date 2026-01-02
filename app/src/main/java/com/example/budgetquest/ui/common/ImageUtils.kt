package com.example.budgetquest.ui.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    // 建立一個暫存的 Uri 給相機使用
    fun createTempPictureUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"

        // 放在 cache 目錄，拍完如果沒存檔會被自動清掉
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            context.cacheDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // 必須跟 Manifest 的 authorities 一致
            image
        )
    }

    // 將 Uri (不論是來自相簿還是相機) 的內容複製到 App 內部的 files 目錄
    // 這樣可以確保圖片長期存在，且不需要一直向系統要權限
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timeStamp.jpg"

            // 建立內部儲存檔案
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // 回傳檔案的絕對路徑 (String)，這就是我們要存入資料庫的值
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}