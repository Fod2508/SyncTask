package com.phuc.synctask.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.phuc.synctask.R;
import com.phuc.synctask.model.Task;

import java.util.List;

/**
 * Adapter cho RecyclerView hiển thị danh sách Task.
 * Mỗi item sử dụng CardView layout cơ bản.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    // Danh sách task hiển thị
    private List<Task> taskList;

    // Context để truy cập resources
    private final Context context;

    // Interface callback cho các sự kiện click
    private final OnTaskActionListener listener;

    /**
     * Nếu true → ẩn tvAssignee (dùng cho danh sách Cá nhân,
     * vì ngữ cảnh đã mặc định là của người dùng hiện tại).
     * Mặc định: false (hiển thị assignee).
     */
    private boolean hideAssignee = false;

    /**
     * Interface xử lý các hành động trên task item.
     */
    public interface OnTaskActionListener {
        /** Khi click checkbox hoàn thành task */
        void onTaskCompleteToggle(Task task, boolean isChecked);

        /** Khi click nút xóa task */
        void onTaskDelete(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    /**
     * Bật/tắt ẩn tên người phụ trách.
     * Gọi setHideAssignee(true) cho RecyclerView "Việc Cá nhân"
     * vì thông tin assignee là dư thừa ở khu vực này.
     *
     * @param hide true = ẩn tvAssignee (View.GONE)
     */
    public void setHideAssignee(boolean hide) {
        this.hideAssignee = hide;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Gán dữ liệu vào các view
        holder.tvTitle.setText(task.getTitle());
        holder.tvDueDate.setText("📅 " + task.getDueDate());
        holder.tvPriority.setText(task.getPriority());
        holder.tvEffort.setText("Effort: " + task.getEffort());
        holder.tvTaskType.setText(task.getTaskType());
        holder.cbCompleted.setChecked(task.isCompleted());

        // ═══ Ẩn/Hiện tên người phụ trách ═══
        if (hideAssignee) {
            holder.tvAssignee.setVisibility(View.GONE);
        } else {
            holder.tvAssignee.setVisibility(View.VISIBLE);
            holder.tvAssignee.setText("👤 " + task.getAssignee());
        }

        // Gạch ngang tiêu đề nếu task đã hoàn thành
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(1.0f);
        }

        // Đổi màu badge priority theo mức độ
        int priorityColor;
        switch (task.getPriority()) {
            case "High":
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                break;
            case "Medium":
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
                break;
            default: // Low
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                break;
        }
        holder.tvPriority.setTextColor(priorityColor);

        // ══════════════════════════════════════════════
        // Nhãn (Tag) phân biệt loại công việc
        // ══════════════════════════════════════════════
        if ("Group".equals(task.getTaskType())) {
            // Task từ nhóm → nhãn "Team" màu xanh dương
            holder.tvTaskType.setText("Team");
            holder.tvTaskType.setTextColor(Color.WHITE);
            holder.tvTaskType.setBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark));
            holder.tvTaskType.setPadding(16, 4, 16, 4);
        } else {
            // Task cá nhân → nhãn "Personal" màu xanh lá
            holder.tvTaskType.setText("Personal");
            holder.tvTaskType.setTextColor(Color.WHITE);
            holder.tvTaskType.setBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.tvTaskType.setPadding(16, 4, 16, 4);
        }

        // Xử lý sự kiện checkbox hoàn thành
        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskCompleteToggle(task, isChecked);
            }
        });

        // Xử lý sự kiện nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    /**
     * Cập nhật danh sách task mới và làm mới RecyclerView.
     *
     * @param newTasks Danh sách Task mới
     */
    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder giữ tham chiếu đến các view trong mỗi item task.
     */
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CheckBox cbCompleted;
        TextView tvTitle;
        TextView tvDueDate;
        TextView tvPriority;
        TextView tvEffort;
        TextView tvTaskType;
        TextView tvAssignee;
        ImageButton btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardTask);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvEffort = itemView.findViewById(R.id.tvEffort);
            tvTaskType = itemView.findViewById(R.id.tvTaskType);
            tvAssignee = itemView.findViewById(R.id.tvAssignee);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
