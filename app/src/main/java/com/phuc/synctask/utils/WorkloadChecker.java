package com.phuc.synctask.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.phuc.synctask.R;
import com.phuc.synctask.data.TaskDao;
import com.phuc.synctask.model.Task;

/**
 * Utility class xử lý logic kiểm tra tải công việc (Workload Analytics).
 *
 * Mỗi khi thêm task mới (từ UI hoặc import JSON),
 * kiểm tra tổng effort trong ngày. Nếu >= 7, cảnh báo quá tải bằng Toast đỏ.
 *
 * Version 2: Toast custom màu đỏ theo yêu cầu.
 */
public class WorkloadChecker {

    // Ngưỡng effort tối đa cho một ngày
    private static final int EFFORT_THRESHOLD = 7;

    // Tên người dùng hiện tại
    public static final String CURRENT_USER = "Phuc";

    /**
     * Kiểm tra tải công việc sau khi insert task mới.
     * Hiển thị Toast MÀU ĐỎ nếu tổng effort trong ngày >= 7.
     *
     * @param context Context hiện tại (Activity/Fragment)
     * @param taskDao TaskDao để truy vấn
     * @param task    Task vừa được insert
     * @return true nếu bị quá tải (effort >= 7), false nếu bình thường
     */
    public static boolean checkAndWarn(Context context, TaskDao taskDao, Task task) {
        // Tính tổng effort của ngày chứa task vừa thêm
        int totalEffort = taskDao.getTotalEffortByDate(task.getDueDate());

        // Kiểm tra ngưỡng quá tải
        if (totalEffort >= EFFORT_THRESHOLD) {
            String message = "⚠️ Quá tải trọng số! Ngày " + task.getDueDate() +
                    " đã có tổng effort là " + totalEffort +
                    ". Vui lòng sắp xếp lại lịch trình.";

            // Hiển thị Toast custom màu đỏ
            showRedToast(context, message);
            return true; // Có quá tải
        }
        return false; // Không quá tải
    }

    /**
     * Hiển thị Toast custom với nền MÀU ĐỎ và chữ trắng.
     * Nổi bật để cảnh báo người dùng ngay lập tức.
     *
     * @param context Context hiện tại
     * @param message Nội dung cảnh báo
     */
    public static void showRedToast(Context context, String message) {
        // Tạo Toast custom
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);

        // Tạo view custom cho Toast
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14);
        textView.setBackgroundColor(Color.parseColor("#D32F2F")); // Material Red 700
        textView.setPadding(32, 24, 32, 24);

        toast.setView(textView);
        toast.show();
    }

    /**
     * Kiểm tra quá tải và hiện AlertDialog (dùng khi cần confirm từ user).
     *
     * @param context Context hiện tại
     * @param taskDao TaskDao để truy vấn
     * @param task    Task vừa insert
     * @return true nếu quá tải
     */
    public static boolean checkAndWarnDialog(Context context, TaskDao taskDao, Task task) {
        int totalEffort = taskDao.getTotalEffortByDate(task.getDueDate());

        if (totalEffort >= EFFORT_THRESHOLD) {
            String message = "Quá tải trọng số! Ngày " + task.getDueDate() +
                    " đã có tổng effort là " + totalEffort +
                    ". Vui lòng sắp xếp lại lịch trình.";

            new AlertDialog.Builder(context)
                    .setTitle("⚠️ Cảnh báo quá tải")
                    .setMessage(message)
                    .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .show();
            return true;
        }
        return false;
    }

    /**
     * Lấy tổng effort của một ngày (không hiển thị cảnh báo).
     *
     * @param taskDao TaskDao
     * @param date    Ngày cần kiểm tra (YYYY-MM-DD)
     * @return Tổng effort trong ngày
     */
    public static int getTotalEffort(TaskDao taskDao, String date) {
        return taskDao.getTotalEffortByDate(date);
    }
}
