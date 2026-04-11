package com.phuc.synctask.utils

import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.phuc.synctask.model.Task
import java.io.*

/**
 * Utility class xử lý Import/Export JSON (Offline Sync).
 */
object JsonSyncHelper {

    private const val TAG = "SyncTask"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export danh sách Task từ RAM ra file JSON.
     */
    fun exportFromTempList(tempTaskList: List<Task>?, projectName: String): File? {
        return try {
            if (tempTaskList.isNullOrEmpty()) {
                Log.e(TAG, "Export thất bại: tempTaskList rỗng.")
                return null
            }

            val jsonString = gson.toJson(tempTaskList)
            val safeFileName = projectName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .lowercase() + "_plan.json"

            val downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val exportFile = File(downloadDir, safeFileName)
            FileWriter(exportFile).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            Log.d(TAG, "Export thành công: ${exportFile.absolutePath} (${tempTaskList.size} task)")
            exportFile
        } catch (e: IOException) {
            Log.e(TAG, "Lỗi Export I/O: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi Export: ${e.message}")
            null
        }
    }

    /**
     * Parse file JSON và ép taskType = "Group" cho TẤT CẢ task.
     */
    fun parseAllAsGroup(file: File): List<Task> {
        val resultTasks = mutableListOf<Task>()
        try {
            val jsonString = BufferedReader(FileReader(file)).use { reader ->
                reader.readText()
            }

            val taskListType = object : TypeToken<List<Task>>() {}.type
            val allTasks: List<Task>? = gson.fromJson(jsonString, taskListType)

            if (allTasks.isNullOrEmpty()) {
                Log.e(TAG, "File JSON không có dữ liệu.")
                return resultTasks
            }

            for (task in allTasks) {
                task.taskType = "Group"
                task.id = 0
                resultTasks.add(task)
            }

            Log.d(TAG, "Parse thành công: ${resultTasks.size} task (tất cả ép taskType=Group)")
        } catch (e: IOException) {
            Log.e(TAG, "Lỗi đọc file: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse JSON: ${e.message}")
        }
        return resultTasks
    }

    /**
     * Lấy danh sách file JSON trong thư mục Download.
     */
    fun getJsonFilesInDownload(): List<File> {
        val jsonFiles = mutableListOf<File>()
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val files = downloadDir.listFiles { _, name ->
                    name.lowercase().endsWith(".json")
                }
                files?.forEach { jsonFiles.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi liệt kê file: ${e.message}")
        }
        return jsonFiles
    }
}
