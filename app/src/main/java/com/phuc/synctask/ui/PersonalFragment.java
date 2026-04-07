package com.phuc.synctask.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.phuc.synctask.R;
import com.phuc.synctask.adapter.ProjectSummaryAdapter;
import com.phuc.synctask.adapter.TaskAdapter;
import com.phuc.synctask.data.AppDatabase;
import com.phuc.synctask.data.TaskDao;
import com.phuc.synctask.model.Task;
import com.phuc.synctask.utils.JsonSyncHelper;
import com.phuc.synctask.utils.WorkloadChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment Tab "Cá nhân" (Personal).
 *
 * Version 7: SharedPreferences lưu tên người dùng + Conditional Query
 * ──────────────────────────────────────────────────────────────────
 * State Management:
 * - Tên người dùng được lưu vào SharedPreferences (key: PREF_USER_NAME)
 * - Khi mở app lần sau, tự động lấy tên đã lưu → lọc công việc
 *
 * CardView 1 (Việc Cá nhân):
 *   - Nếu currentUserName rỗng → observe getAllPersonalTasks()
 *   - Nếu currentUserName có dữ liệu → observe getPersonalAndAssignedTasks(name)
 *   - Dùng switchMap: thay đổi tên → tự đổi query
 *
 * CardView 2 (Việc từ Nhóm — Master-Detail):
 *   - Nút Import JSON (luồng 3 bước + lưu SharedPreferences)
 *   - RecyclerView #2 (ProjectSummaryAdapter)
 *   - Detail Dialog có TableLayout + cột Xóa (🗑️)
 * ──────────────────────────────────────────────────────────────────
 */
public class PersonalFragment extends Fragment
        implements TaskAdapter.OnTaskActionListener,
        ProjectSummaryAdapter.OnProjectClickListener {

    private static final String TAG = "SyncTask";

    // ========================
    // SharedPreferences Constants
    // ========================
    private static final String PREFS_NAME = "SyncTaskPrefs";
    private static final String PREF_USER_NAME = "PREF_USER_NAME";

    // Threading
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Database
    private TaskDao taskDao;

    // Tên người dùng (khôi phục từ SharedPreferences hoặc nhập mới)
    private String currentUserName = "";

    /**
     * MutableLiveData chứa tên người dùng.
     *
     * switchMap logic:
     * - Nếu value = "" → observe getAllPersonalTasks() (chỉ Personal)
     * - Nếu value != "" → observe getPersonalAndAssignedTasks(name)
     *     (Personal + Group assigned cho user)
     */
    private final MutableLiveData<String> userNameLiveData = new MutableLiveData<>("");

    // UI — CardView 1
    private TextInputEditText etPersonalTitle, etPersonalDueDate;
    private AutoCompleteTextView actvPersonalEffort;
    private RecyclerView rvPersonalTasks;
    private TaskAdapter personalAdapter;
    private TextView tvPersonalCount, tvPersonalEmpty;

    // UI — CardView 2 (Master)
    private RecyclerView rvGroupProjects;
    private ProjectSummaryAdapter projectSummaryAdapter;
    private TextView tvGroupCount, tvGroupEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskDao = AppDatabase.getInstance(requireContext()).taskDao();

        // ════════════════════════════════════════════════════════════
        // BƯỚC 1: Khôi phục tên từ SharedPreferences
        // ════════════════════════════════════════════════════════════
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUserName = prefs.getString(PREF_USER_NAME, "");
        userNameLiveData.setValue(currentUserName);

        Log.d(TAG, "Khôi phục currentUserName từ SharedPreferences: \""
                + currentUserName + "\"");

        // ========================
        // CardView 1: Việc Cá nhân
        // ========================
        etPersonalTitle = view.findViewById(R.id.etPersonalTitle);
        etPersonalDueDate = view.findViewById(R.id.etPersonalDueDate);
        actvPersonalEffort = view.findViewById(R.id.actvPersonalEffort);

        String[] efforts = {"1", "2", "3"};
        ArrayAdapter<String> effortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, efforts);
        actvPersonalEffort.setAdapter(effortAdapter);
        actvPersonalEffort.setText("1", false); // Giá trị mặc định

        etPersonalDueDate.setOnClickListener(v -> showDatePickerDialog(etPersonalDueDate));
        view.findViewById(R.id.btnAddPersonalTask).setOnClickListener(v -> addPersonalTask());

        tvPersonalCount = view.findViewById(R.id.tvPersonalCount);
        tvPersonalEmpty = view.findViewById(R.id.tvPersonalEmpty);
        rvPersonalTasks = view.findViewById(R.id.rvPersonalTasks);

        personalAdapter = new TaskAdapter(requireContext(), new ArrayList<>(), this);
        personalAdapter.setHideAssignee(true); // Ẩn tên assignee (dư thừa ở danh sách cá nhân)
        rvPersonalTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersonalTasks.setAdapter(personalAdapter);

        // ========================
        // CardView 2: Việc từ Nhóm — Master
        // ========================
        tvGroupCount = view.findViewById(R.id.tvGroupCount);
        tvGroupEmpty = view.findViewById(R.id.tvGroupEmpty);
        rvGroupProjects = view.findViewById(R.id.rvGroupTasks);

        projectSummaryAdapter = new ProjectSummaryAdapter(requireContext(), this, taskDao);
        rvGroupProjects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGroupProjects.setAdapter(projectSummaryAdapter);

        view.findViewById(R.id.btnImportJson).setOnClickListener(v -> showNameInputDialog());

        // ════════════════════════════════════════════════════════════
        // OBSERVER 1: RV #1 — Conditional Query (switchMap)
        // ════════════════════════════════════════════════════════════
        //
        // switchMap logic:
        //   userName = "" → getAllPersonalTasks()    (chỉ Personal)
        //   userName != "" → getPersonalAndAssignedTasks(name)
        //                    (Personal + Group assigned)
        //
        // Khi userNameLiveData.setValue() thay đổi → tự hủy query cũ
        // → gọi query mới → RecyclerView tự cập nhật.
        LiveData<List<Task>> personalLive = Transformations.switchMap(
                userNameLiveData,
                userName -> {
                    if (userName == null || userName.isEmpty()) {
                        // Chưa nhập tên → chỉ hiện task Personal
                        Log.d(TAG, "switchMap: userName rỗng → getAllPersonalTasks()");
                        return taskDao.getAllPersonalTasks();
                    } else {
                        // Đã nhập tên → Personal + Group assigned
                        Log.d(TAG, "switchMap: userName=\"" + userName
                                + "\" → getPersonalAndAssignedTasks()");
                        return taskDao.getPersonalAndAssignedTasks(userName);
                    }
                }
        );

        personalLive.observe(getViewLifecycleOwner(), tasks -> {
            personalAdapter.updateTasks(tasks != null ? tasks : new ArrayList<>());

            int count = (tasks != null) ? tasks.size() : 0;
            tvPersonalCount.setText("Danh sách: (" + count + " việc)");

            if (tasks == null || tasks.isEmpty()) {
                tvPersonalEmpty.setVisibility(View.VISIBLE);
                rvPersonalTasks.setVisibility(View.GONE);
            } else {
                tvPersonalEmpty.setVisibility(View.GONE);
                rvPersonalTasks.setVisibility(View.VISIBLE);
            }
        });

        // ════════════════════════════════════════════════════════════
        // OBSERVER 2: RV #2 — Master (gom nhóm theo projectName)
        // ════════════════════════════════════════════════════════════
        taskDao.getAllGroupTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks == null || tasks.isEmpty()) {
                tvGroupCount.setText("Đã nhận: (0 đề tài)");
                tvGroupEmpty.setVisibility(View.VISIBLE);
                rvGroupProjects.setVisibility(View.GONE);
                projectSummaryAdapter.updateData(new LinkedHashMap<>());
            } else {
                Map<String, List<Task>> projectMap = groupByProject(tasks);

                tvGroupCount.setText("Đã nhận: " + projectMap.size() + " đề tài ("
                        + tasks.size() + " việc)");
                tvGroupEmpty.setVisibility(View.GONE);
                rvGroupProjects.setVisibility(View.VISIBLE);

                projectSummaryAdapter.updateData(projectMap);
            }
        });
    }

    // ================================================================
    // SharedPreferences: Lưu tên người dùng
    // ================================================================

    /**
     * Lưu tên người dùng vào SharedPreferences.
     * Gọi sau khi user nhập tên thành công trong dialog Import.
     *
     * @param userName Tên người dùng cần lưu
     */
    private void saveUserName(String userName) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_USER_NAME, userName).apply();
        Log.d(TAG, "Đã lưu userName vào SharedPreferences: \"" + userName + "\"");
    }

    // ================================================================
    // DatePickerDialog
    // ================================================================

    private void showDatePickerDialog(TextInputEditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    targetEditText.setText(formattedDate);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // ================================================================
    // DATA AGGREGATION
    // ================================================================

    private Map<String, List<Task>> groupByProject(List<Task> tasks) {
        Map<String, List<Task>> projectMap = new LinkedHashMap<>();
        for (Task task : tasks) {
            String projectName = task.getProjectName();
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = "(Không có tên)";
            }
            if (!projectMap.containsKey(projectName)) {
                projectMap.put(projectName, new ArrayList<>());
            }
            projectMap.get(projectName).add(task);
        }
        Log.d(TAG, "groupByProject: " + projectMap.size() + " đề tài từ "
                + tasks.size() + " task");
        return projectMap;
    }

    // ================================================================
    // DETAIL DIALOG
    // ================================================================

    @Override
    public void onProjectClick(String projectName, List<Task> tasks) {
        showProjectDetailDialog(projectName, tasks);
    }

    private void showProjectDetailDialog(String projectName, List<Task> tasks) {
        final List<Task> mutableTasks = new ArrayList<>(tasks);

        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(16, 16, 16, 16);

        TextView tvSummary = new TextView(requireContext());
        tvSummary.setText("📊 Tổng cộng: " + mutableTasks.size() + " đầu việc");
        tvSummary.setTextSize(14);
        tvSummary.setTextColor(Color.parseColor("#616161"));
        tvSummary.setPadding(0, 0, 0, 16);
        rootLayout.addView(tvSummary);

        HorizontalScrollView scrollView = new HorizontalScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TableLayout tableLayout = new TableLayout(requireContext());
        tableLayout.setStretchAllColumns(true);
        tableLayout.setPadding(0, 0, 0, 8);

        // Header
        TableRow headerRow = new TableRow(requireContext());
        headerRow.setBackgroundColor(Color.parseColor("#1976D2"));
        headerRow.setPadding(4, 8, 4, 8);
        headerRow.addView(createHeaderCell("STT"));
        headerRow.addView(createHeaderCell("Tên Việc"));
        headerRow.addView(createHeaderCell("Người đảm nhiệm"));
        headerRow.addView(createHeaderCell("Hạn chót"));
        headerRow.addView(createHeaderCell("Xóa"));
        tableLayout.addView(headerRow);

        // Data Rows
        for (int i = 0; i < mutableTasks.size(); i++) {
            final Task task = mutableTasks.get(i);

            TableRow dataRow = new TableRow(requireContext());
            dataRow.setBackgroundColor(i % 2 == 0 ?
                    Color.parseColor("#FFFFFF") : Color.parseColor("#F5F5F5"));
            dataRow.setPadding(4, 6, 4, 6);
            dataRow.setGravity(Gravity.CENTER_VERTICAL);

            dataRow.addView(createDataCell(String.valueOf(i + 1), Gravity.CENTER));
            dataRow.addView(createDataCell(task.getTitle(), Gravity.START));

            String assignee = (task.getAssignee() != null) ? task.getAssignee() : "—";
            dataRow.addView(createDataCell(assignee, Gravity.CENTER));

            String dueDate = (task.getDueDate() != null) ? task.getDueDate() : "—";
            dataRow.addView(createDataCell(dueDate, Gravity.CENTER));

            ImageButton btnDelete = new ImageButton(requireContext());
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
            btnDelete.setBackgroundColor(Color.TRANSPARENT);
            btnDelete.setPadding(8, 4, 8, 4);
            btnDelete.setContentDescription("Xóa công việc");

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("🗑️ Xóa công việc")
                        .setMessage("Xóa \"" + task.getTitle() + "\" khỏi database?")
                        .setPositiveButton("Xóa", (dialog2, which2) -> {
                            executor.execute(() -> {
                                try {
                                    taskDao.deleteTask(task);
                                    Log.d(TAG, "Deleted from Room: " + task.getTitle());
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            tableLayout.removeView(dataRow);
                                            mutableTasks.remove(task);
                                            tvSummary.setText("📊 Tổng cộng: "
                                                    + mutableTasks.size() + " đầu việc");
                                            toast("Đã xóa công việc: " + task.getTitle());
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi xóa task: " + e.getMessage());
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                toast("❌ Lỗi xóa: " + e.getMessage()));
                                    }
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            dataRow.addView(btnDelete);
            tableLayout.addView(dataRow);
        }

        scrollView.addView(tableLayout);
        rootLayout.addView(scrollView);

        new AlertDialog.Builder(requireContext())
                .setTitle("📂 " + projectName)
                .setView(rootLayout)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private TextView createHeaderCell(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(12, 8, 12, 8);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView createDataCell(String text, int gravity) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#212121"));
        tv.setTextSize(13);
        tv.setPadding(12, 8, 12, 8);
        tv.setGravity(gravity);
        tv.setMaxLines(2);
        return tv;
    }

    // ================================================================
    // Thêm việc cá nhân
    // ================================================================

    private void addPersonalTask() {
        String title = getText(etPersonalTitle);
        String dueDate = getText(etPersonalDueDate);
        String effortText = actvPersonalEffort.getText().toString().trim();
        int effort = effortText.isEmpty() ? 1 : Integer.parseInt(effortText);

        if (title.isEmpty()) {
            toast("Vui lòng nhập tên việc!");
            etPersonalTitle.requestFocus();
            return;
        }
        if (dueDate.isEmpty()) {
            toast("Vui lòng chọn hạn chót!");
            etPersonalDueDate.performClick();
            return;
        }

        Task newTask = new Task(
                title, dueDate, "Medium", effort,
                false, "Personal", null,
                currentUserName.isEmpty() ? "Personal" : currentUserName
        );

        executor.execute(() -> {
            try {
                taskDao.insertTask(newTask);
                Log.d(TAG, "Insert Personal thành công: " + newTask);

                int totalEffort = taskDao.getTotalEffortByDate(dueDate);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        toast("✅ Đã thêm: " + title);

                        if (totalEffort >= 7) {
                            WorkloadChecker.showRedToast(requireContext(),
                                    "⚠️ Quá tải! Ngày " + dueDate
                                            + " tổng effort = " + totalEffort);
                        }

                        etPersonalTitle.setText("");
                        etPersonalDueDate.setText("");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi insert Personal: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            toast("❌ Lỗi thêm task: " + e.getMessage()));
                }
            }
        });
    }

    // ================================================================
    // Import JSON — Luồng 3 bước + lưu SharedPreferences
    // ================================================================

    /**
     * BƯỚC 1: AlertDialog nhập tên người dùng.
     * Tên đã lưu trong SharedPreferences → điền sẵn vào EditText.
     */
    private void showNameInputDialog() {
        final EditText editTextName = new EditText(requireContext());
        editTextName.setHint("Ví dụ: Phuc, Minh, Lan...");
        editTextName.setSingleLine(true);
        editTextName.setPadding(48, 32, 48, 16);

        // Điền sẵn tên đã lưu (từ SharedPreferences hoặc lần nhập trước)
        if (!currentUserName.isEmpty()) {
            editTextName.setText(currentUserName);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("📝 Nhập tên của bạn để hệ thống lọc công việc:")
                .setView(editTextName)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String inputName = editTextName.getText().toString().trim();
                    if (inputName.isEmpty()) {
                        toast("Vui lòng nhập tên!");
                        return;
                    }

                    // BƯỚC 2: Lưu tên → cập nhật state
                    currentUserName = inputName;

                    // Lưu vào SharedPreferences (persistent)
                    saveUserName(currentUserName);

                    // Cập nhật MutableLiveData → switchMap re-query
                    userNameLiveData.setValue(currentUserName);

                    Log.d(TAG, "currentUserName = \"" + currentUserName
                            + "\" (đã lưu SharedPreferences)");
                    toast("Xin chào " + currentUserName + "! Chọn file JSON...");

                    showFilePickerDialog();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showFilePickerDialog() {
        List<File> jsonFiles = JsonSyncHelper.getJsonFilesInDownload();

        if (jsonFiles.isEmpty()) {
            toast("Không tìm thấy file JSON trong thư mục Download!");
            return;
        }

        String[] fileNames = new String[jsonFiles.size()];
        for (int i = 0; i < jsonFiles.size(); i++) {
            fileNames[i] = jsonFiles.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("📥 Chọn file JSON để Import")
                .setItems(fileNames, (dialog, which) -> {
                    File selectedFile = jsonFiles.get(which);
                    performImport(selectedFile);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * BƯỚC 3: Parse JSON → ép taskType="Group" → insert ALL.
     * LiveData tự cập nhật cả 2 RecyclerView.
     */
    private void performImport(File file) {
        executor.execute(() -> {
            try {
                List<Task> allGroupTasks = JsonSyncHelper.parseAllAsGroup(file);

                int insertedCount = 0;
                for (Task task : allGroupTasks) {
                    taskDao.insertTask(task);
                    insertedCount++;
                }

                final int count = insertedCount;
                Log.d(TAG, "Import hoàn tất: " + count + " task từ " + file.getName());

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (count > 0) {
                            toast("📥 Import thành công! " + count + " việc đã lưu.");
                        } else {
                            toast("File JSON không có dữ liệu.");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi import: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            toast("❌ Lỗi import: " + e.getMessage()));
                }
            }
        });
    }

    // ================================================================
    // TaskAdapter Callbacks
    // ================================================================

    @Override
    public void onTaskCompleteToggle(Task task, boolean isChecked) {
        executor.execute(() -> {
            try {
                task.setCompleted(isChecked);
                taskDao.updateTask(task);
                Log.d(TAG, "Toggle complete: " + task.getTitle() + " → " + isChecked);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi update task: " + e.getMessage());
            }
        });
    }

    @Override
    public void onTaskDelete(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Xác nhận xóa")
                .setMessage("Xóa task \"" + task.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    executor.execute(() -> {
                        try {
                            taskDao.deleteTask(task);
                            Log.d(TAG, "Deleted: " + task.getTitle());
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        toast("Đã xóa: " + task.getTitle()));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi xóa task: " + e.getMessage());
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================================================================
    // Helper
    // ================================================================

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
