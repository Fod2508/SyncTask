package com.phuc.synctask.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.phuc.synctask.R;
import com.phuc.synctask.model.Task;

import java.util.List;

/**
 * Adapter cho danh sách đầu việc trong Tab Nhóm.
 * Hiển thị tên việc, người phụ trách, hạn chót, priority, effort.
 */
public class GroupTaskAdapter extends RecyclerView.Adapter<GroupTaskAdapter.GroupTaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final OnGroupTaskActionListener listener;

    /**
     * Interface callback khi xóa đầu việc nhóm.
     */
    public interface OnGroupTaskActionListener {
        void onGroupTaskDelete(Task task, int position);
    }

    public GroupTaskAdapter(Context context, List<Task> taskList,
                            OnGroupTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_task, parent, false);
        return new GroupTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupTaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvAssignee.setText("👤 " + task.getAssignee());
        holder.tvDueDate.setText("📅 " + task.getDueDate());
        holder.tvPriority.setText(task.getPriority());
        holder.tvEffort.setText("Effort: " + task.getEffort());

        // Màu priority
        int priorityColor;
        switch (task.getPriority()) {
            case "High":
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                break;
            case "Medium":
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
                break;
            default:
                priorityColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
                break;
        }
        holder.tvPriority.setTextColor(priorityColor);

        // Xử lý nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupTaskDelete(task, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    /**
     * Cập nhật danh sách task.
     */
    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    static class GroupTaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAssignee, tvDueDate, tvPriority, tvEffort;
        ImageButton btnDelete;

        GroupTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGroupTaskTitle);
            tvAssignee = itemView.findViewById(R.id.tvGroupAssignee);
            tvDueDate = itemView.findViewById(R.id.tvGroupDueDate);
            tvPriority = itemView.findViewById(R.id.tvGroupPriority);
            tvEffort = itemView.findViewById(R.id.tvGroupEffort);
            btnDelete = itemView.findViewById(R.id.btnDeleteGroupTask);
        }
    }
}
