package com.phuc.synctask.utils;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.phuc.synctask.model.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class xử lý Import/Export JSON (Offline Sync).
 *
 * Version 3 — Separation of Concerns:
 * - Export: Nhận ArrayList<Task> từ RAM (tempTaskList), KHÔNG truy vấn Room DB.
 * - Import: Parse file JSON → Trả về List<Task> đã lọc → Caller tự insert vào Room.
 * - Tất cả I/O file phải được gọi trên Background Thread (caller chịu trách nhiệm).
 */
public class JsonSyncHelper {

    private static final String TAG = "SyncTask";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Export danh sách Task từ RAM (tempTaskList) ra file JSON.
     * KHÔNG truy vấn Room DB.
     *
     * Phải gọi trên Background Thread (ExecutorService).
     *
     * @param tempTaskList ArrayList chứa các Task tạm trong RAM
     * @param projectName  Tên dự án (dùng đặt tên file)
     * @return File đã ghi nếu thành công, null nếu thất bại
     */
    public static File exportFromTempList(List<Task> tempTaskList, String projectName) {
        try {
            if (tempTaskList == null || tempTaskList.isEmpty()) {
                Log.e(TAG, "Export thất bại: tempTaskList rỗng.");
                return null;
            }

            // Chuyển đổi sang JSON
            String jsonString = gson.toJson(tempTaskList);

            // Tạo tên file an toàn
            String safeFileName = projectName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    .toLowerCase() + "_plan.json";

            // Ghi ra thư mục Download
            File downloadDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File exportFile = new File(downloadDir, safeFileName);
            FileWriter writer = new FileWriter(exportFile);
            writer.write(jsonString);
            writer.flush();
            writer.close();

            Log.d(TAG, "Export thành công: " + exportFile.getAbsolutePath()
                    + " (" + tempTaskList.size() + " task)");
            return exportFile;

        } catch (IOException e) {
            Log.e(TAG, "Lỗi Export I/O: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi Export: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse file JSON và ép taskType = "Group" cho TẤT CẢ task.
     * KHÔNG lọc theo assignee — lưu hết vào danh sách trả về.
     * Reset id = 0 để Room auto-generate khi insert.
     *
     * Phải gọi trên Background Thread (ExecutorService).
     *
     * @param file File JSON cần đọc
     * @return Danh sách TOÀN BỘ Task (đã set taskType = "Group", id = 0)
     */
    public static List<Task> parseAllAsGroup(File file) {
        List<Task> resultTasks = new ArrayList<>();

        try {
            // Đọc file JSON
            StringBuilder jsonBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            String jsonString = jsonBuilder.toString();

            // Parse JSON → List<Task> bằng Gson + TypeToken
            Type taskListType = new TypeToken<List<Task>>() {}.getType();
            List<Task> allTasks = gson.fromJson(jsonString, taskListType);

            if (allTasks == null || allTasks.isEmpty()) {
                Log.e(TAG, "File JSON không có dữ liệu.");
                return resultTasks;
            }

            // Ép taskType = "Group" cho TẤT CẢ task (không lọc if-else)
            for (Task task : allTasks) {
                task.setTaskType("Group");
                task.setId(0); // Reset để Room auto-generate
                resultTasks.add(task);
            }

            Log.d(TAG, "Parse thành công: " + resultTasks.size() +
                    " task (tất cả ép taskType=Group)");

        } catch (IOException e) {
            Log.e(TAG, "Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse JSON: " + e.getMessage());
        }

        return resultTasks;
    }

    /**
     * Lấy danh sách tất cả file JSON trong thư mục Download.
     *
     * @return Danh sách File JSON
     */
    public static List<File> getJsonFilesInDownload() {
        List<File> jsonFiles = new ArrayList<>();
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            if (downloadDir.exists() && downloadDir.isDirectory()) {
                File[] files = downloadDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        jsonFiles.add(f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi liệt kê file: " + e.getMessage());
        }
        return jsonFiles;
    }
}
