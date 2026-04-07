package com.phuc.synctask.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.phuc.synctask.ui.GroupFragment;
import com.phuc.synctask.ui.PersonalFragment;

/**
 * Adapter cho ViewPager2 quản lý 2 tab:
 * - Tab 0: PersonalFragment (Cá nhân)
 * - Tab 1: GroupFragment (Quản lý Nhóm)
 */
public class ViewPagerAdapter extends FragmentStateAdapter {

    // Số lượng tab
    private static final int NUM_TABS = 2;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PersonalFragment();
            case 1:
                return new GroupFragment();
            default:
                return new PersonalFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}
