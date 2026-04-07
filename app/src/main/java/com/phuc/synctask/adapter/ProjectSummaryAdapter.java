package com.phuc.synctask.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.phuc.synctask.R;
import com.phuc.synctask.data.TaskDao;
import com.phuc.synctask.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter cho RecyclerView "Việc từ Nhóm" — Master View.
 *
 * Version 2: Thêm chức năng Xóa toàn bộ dự án nhóm.
 *
 * Mỗi item gồm: 📂 icon + Tên đề tài (bold) + Số đầu việc + 🗑️ Xóa + ▶ Chi tiết
 *
 * Xóa dự án:
 * - Click 🗑️ → AlertDialog xác nhận → taskDao.deleteProject() trên Background Thread
 * - LiveData tự cập nhật RecyclerView (không cần gọi thủ công)
 */
public class ProjectSummaryAdapter
        extends RecyclerView.Adapter<ProjectSummaryAdapter.ProjectViewHolder> {

    private static final String TAG = "SyncTask";

    // ========================
    // Interface Callbacks
    // ========================

    /**
     * Callback khi bấm vào item → mở Detail Dialog.
     */
    public interface OnProjectClickListener {
        void onProjectClick(String projectName, List<Task> tasks);
    }

    // ========================
    // Data
    // ========================
    private List<String> projectNames = new ArrayList<>();
    private Map<String, List<Task>> projectMap;

    private final Context context;
    private final OnProjectClickListener clickListener;
    private final TaskDao taskDao;

    /** Background thread cho việc xóa từ Room DB */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ========================
    // Constructor
    // ========================

    /**
     * @param context       Context
     * @param clickListener Callback khi bấm vào project (mở detail)
     * @param taskDao       TaskDao để gọi deleteProject() trên background thread
     */
    public ProjectSummaryAdapter(Context context,
                                  OnProjectClickListener clickListener,
                                  TaskDao taskDao) {
        this.context = context;
        this.clickListener = clickListener;
        this.taskDao = taskDao;
    }

    // ========================
    // Cập nhật dữ liệu
    // ========================

    public void updateData(Map<String, List<Task>> projectMap) {
        this.projectMap = projectMap;
        this.projectNames = new ArrayList<>(projectMap.keySet());
        notifyDataSetChanged();
    }

    // ========================
    // RecyclerView Override
    // ========================

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_summary, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        String projectName = projectNames.get(position);
        List<Task> tasks = projectMap.get(projectName);

        int taskCount = (tasks != null) ? tasks.size() : 0;

        holder.tvProjectName.setText(projectName);
        holder.tvTaskCount.setText(taskCount + " đầu việc");

        // ── Click item → mở Detail Dialog ──
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null && tasks != null) {
                clickListener.onProjectClick(projectName, tasks);
            }
        });

        // ── Click 🗑️ → xóa toàn bộ dự án ──
        holder.btnDeleteProject.setOnClickListener(v -> {
            showDeleteConfirmDialog(projectName, taskCount);
        });
    }

    @Override
    public int getItemCount() {
        return projectNames.size();
    }

    // ================================================================
    // XÓA DỰ ÁN: AlertDialog xác nhận → Background Thread → Room DB
    // ================================================================

    /**
     * Hiển thị AlertDialog xác nhận trước khi xóa toàn bộ dự án.
     *
     * @param projectName Tên dự án cần xóa
     * @param taskCount   Số lượng đầu việc (hiển thị trong message)
     */
    private void showDeleteConfirmDialog(String projectName, int taskCount) {
        new AlertDialog.Builder(context)
                .setTitle("🗑️ Xóa dự án")
                .setMessage("Bạn có chắc chắn muốn xóa toàn bộ dự án \""
                        + projectName + "\" và tất cả " + taskCount
                        + " công việc bên trong không?\n\nHành động này không thể hoàn tác.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    deleteProjectInBackground(projectName);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Gọi taskDao.deleteProject() trên Background Thread.
     * Sau khi xóa xong → LiveData (getAllGroupTasks) tự động
     * trigger Observer trong PersonalFragment → cập nhật RecyclerView.
     *
     * @param projectName Tên dự án cần xóa
     */
    private void deleteProjectInBackground(String projectName) {
        executor.execute(() -> {
            try {
                // Xóa tất cả task có projectName khớp VÀ taskType = "Group"
                taskDao.deleteProject(projectName);
                Log.d(TAG, "Đã xóa toàn bộ dự án: \"" + projectName + "\"");

                // Quay lại UI Thread để hiện Toast
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context,
                                "Đã xóa toàn bộ dự án: " + projectName,
                                Toast.LENGTH_SHORT).show();
                        // LiveData tự cập nhật RecyclerView — không cần gọi thủ công
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi xóa dự án: " + e.getMessage());
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context,
                                "❌ Lỗi xóa: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // ========================
    // ViewHolder
    // ========================

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvProjectIcon;
        TextView tvProjectName;
        TextView tvTaskCount;
        ImageButton btnDeleteProject;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardProjectSummary);
            tvProjectIcon = itemView.findViewById(R.id.tvProjectIcon);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
            btnDeleteProject = itemView.findViewById(R.id.btnDeleteProject);
        }
    }
}
