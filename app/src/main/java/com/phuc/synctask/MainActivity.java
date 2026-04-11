package com.phuc.synctask;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.phuc.synctask.adapter.ViewPagerAdapter;

/**
 * Activity chính của ứng dụng SyncTask.
 *
 * Thiết lập TabLayout + ViewPager2 với 2 tab:
 * - Tab 0: "Cá nhân" (PersonalFragment)
 * - Tab 1: "Quản lý Nhóm" (GroupFragment)
 *
 * Xử lý quyền lưu trữ cho Export/Import JSON.
 */
public class MainActivity extends AppCompatActivity {

    // Mã request quyền bộ nhớ
    private static final int PERMISSION_REQUEST_STORAGE = 100;

    // Tiêu đề của từng tab
    private static final String[] TAB_TITLES = {"📋 Cá nhân", "👥 Quản lý Nhóm"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Xử lý Edge-to-Edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ════════════════════════════════════════════
        // Header tùy chỉnh — không cần setSupportActionBar()
        // (toolbar đã được thay bằng plain LinearLayout header)
        // ════════════════════════════════════════════

        // Thiết lập TabLayout + ViewPager2
        setupTabsAndViewPager();

        // Yêu cầu quyền truy cập bộ nhớ (cho Export/Import)
        requestStoragePermission();
    }

    /**
     * Thiết lập TabLayout liên kết với ViewPager2.
     * Sử dụng TabLayoutMediator để đồng bộ tab title và page position.
     */
    private void setupTabsAndViewPager() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Tạo adapter quản lý các Fragment
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Liên kết TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TAB_TITLES[position]);
        }).attach();
    }

    // ========================
    // Xử lý quyền bộ nhớ
    // ========================

    /**
     * Yêu cầu quyền READ/WRITE_EXTERNAL_STORAGE cho Android < 10.
     * Android 10+ dùng Scoped Storage.
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền truy cập bộ nhớ!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Cần quyền truy cập bộ nhớ để Export/Import JSON!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
