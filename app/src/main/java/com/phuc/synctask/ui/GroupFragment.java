package com.phuc.synctask.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.phuc.synctask.R;
import com.phuc.synctask.adapter.GroupTaskAdapter;
import com.phuc.synctask.model.Task;
import com.phuc.synctask.utils.JsonSyncHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment Tab "Quản lý Nhóm" (Group Planning).
 *
 * Version 6: Thêm DatePickerDialog cho trường Hạn chót.
 * Validation: Không cho chọn ngày trong quá khứ (setMinDate).
 */
public class GroupFragment extends Fragment implements GroupTaskAdapter.OnGroupTaskActionListener {

    private static final String TAG = "SyncTask";

    // Data: Danh sách tạm lưu trong RAM
    private final ArrayList<Task> tempTaskList = new ArrayList<>();

    // Threading
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Components
    private TextInputEditText etProjectName, etTaskTitle, etAssignee, etDueDate;
    private AutoCompleteTextView actvEffort;
    private RecyclerView rvTempTasks;
    private GroupTaskAdapter tempAdapter;
    private TextView tvTempListCount, tvTempEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // CardView 1: Nhập liệu
        etProjectName = view.findViewById(R.id.etProjectName);
        etTaskTitle = view.findViewById(R.id.etTaskTitle);
        etAssignee = view.findViewById(R.id.etAssignee);
        etDueDate = view.findViewById(R.id.etDueDate);
        actvEffort = view.findViewById(R.id.actvEffort);

        // Setup Exposed Dropdown Menu
        String[] efforts = {"1", "2", "3"};
        ArrayAdapter<String> effortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, efforts);
        actvEffort.setAdapter(effortAdapter);
        actvEffort.setText("1", false); // Giá trị mặc định

        // ════════════════════════════════════════════
        // DatePickerDialog: Trường "Hạn chót"
        // ════════════════════════════════════════════
        // EditText đã set focusable=false trong XML
        // → Click mở DatePickerDialog thay vì bàn phím
        etDueDate.setOnClickListener(v -> showDatePickerDialog(etDueDate));

        // Nút "Thêm vào danh sách tạm"
        view.findViewById(R.id.btnAddToTempList).setOnClickListener(v -> addToTempList());

        // CardView 2: Hiển thị & Export
        tvTempListCount = view.findViewById(R.id.tvTempListCount);
        tvTempEmptyState = view.findViewById(R.id.tvTempEmptyState);
        rvTempTasks = view.findViewById(R.id.rvTempTasks);

        tempAdapter = new GroupTaskAdapter(requireContext(), tempTaskList, this);
        rvTempTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTempTasks.setAdapter(tempAdapter);

        view.findViewById(R.id.btnExportJson).setOnClickListener(v -> exportToJson());

        refreshTempListUI();
    }

    // ================================================================
    // DatePickerDialog — Chọn ngày chuẩn YYYY-MM-DD
    // ================================================================

    /**
     * Hiển thị DatePickerDialog mặc định Android.
     * - setMinDate: Ngăn chọn ngày quá khứ
     * - Format: YYYY-MM-DD
     *
     * @param targetEditText EditText sẽ nhận kết quả ngày chọn
     */
    private void showDatePickerDialog(TextInputEditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format: YYYY-MM-DD (month là 0-indexed nên +1)
                    String formattedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    targetEditText.setText(formattedDate);
                },
                year, month, day
        );

        // Validation: Không cho chọn ngày trong quá khứ
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

    // ================================================================
    // Thêm vào danh sách tạm
    // ================================================================

    private void addToTempList() {
        String projectName = getText(etProjectName);
        String taskTitle = getText(etTaskTitle);
        String assignee = getText(etAssignee);
        String dueDate = getText(etDueDate);
        String effortText = actvEffort.getText().toString().trim();
        int effort = effortText.isEmpty() ? 1 : Integer.parseInt(effortText);

        if (projectName.isEmpty()) {
            toast("Vui lòng nhập tên Dự án!");
            etProjectName.requestFocus();
            return;
        }
        if (taskTitle.isEmpty()) {
            toast("Vui lòng nhập tên đầu việc!");
            etTaskTitle.requestFocus();
            return;
        }
        if (assignee.isEmpty()) {
            toast("Vui lòng nhập tên người phụ trách!");
            etAssignee.requestFocus();
            return;
        }
        if (dueDate.isEmpty()) {
            toast("Vui lòng chọn hạn chót!");
            etDueDate.performClick(); // Mở DatePicker luôn
            return;
        }

        Task newTask = new Task(
                taskTitle, dueDate, "Medium", effort,
                false, "Group", projectName, assignee
        );

        tempTaskList.add(newTask);
        refreshTempListUI();

        // Clear form (giữ lại Tên Dự án)
        etTaskTitle.setText("");
        etAssignee.setText("");
        etDueDate.setText("");

        toast("✅ Đã thêm: " + taskTitle + " → " + assignee);
        Log.d(TAG, "Thêm vào tempList: " + newTask + " | Tổng: " + tempTaskList.size());
    }

    // ================================================================
    // Export JSON
    // ================================================================

    private void exportToJson() {
        String projectName = getText(etProjectName);

        if (projectName.isEmpty()) {
            toast("Vui lòng nhập tên Dự án!");
            return;
        }
        if (tempTaskList.isEmpty()) {
            toast("Danh sách tạm trống! Hãy thêm đầu việc trước.");
            return;
        }

        final List<Task> exportList = new ArrayList<>(tempTaskList);
        final String projName = projectName;

        executor.execute(() -> {
            try {
                File result = JsonSyncHelper.exportFromTempList(exportList, projName);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (result != null) {
                            toast("📤 Export thành công!\n" + result.getAbsolutePath()
                                    + "\n(" + exportList.size() + " task)");
                            tempTaskList.clear();
                            refreshTempListUI();
                        } else {
                            toast("❌ Export thất bại! Xem Logcat (tag: SyncTask).");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi export: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            toast("❌ Lỗi export: " + e.getMessage()));
                }
            }
        });
    }

    // ================================================================
    // UI Helper
    // ================================================================

    private void refreshTempListUI() {
        tempAdapter.updateTasks(tempTaskList);
        tvTempListCount.setText("Danh sách tạm: (" + tempTaskList.size() + " việc)");

        if (tempTaskList.isEmpty()) {
            tvTempEmptyState.setVisibility(View.VISIBLE);
            rvTempTasks.setVisibility(View.GONE);
        } else {
            tvTempEmptyState.setVisibility(View.GONE);
            rvTempTasks.setVisibility(View.VISIBLE);
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // ================================================================
    // GroupTaskAdapter Callback: Xóa item khỏi danh sách tạm
    // ================================================================

    @Override
    public void onGroupTaskDelete(Task task, int position) {
        if (position >= 0 && position < tempTaskList.size()) {
            tempTaskList.remove(position);
            tempAdapter.notifyItemRemoved(position);
            tempAdapter.notifyItemRangeChanged(position, tempTaskList.size());
            refreshTempListUI();
            toast("Đã xóa công việc: " + task.getTitle());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
