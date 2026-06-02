package com.filemanager.android.features.admin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new AdminUpgradeRequestsFragment();
        }
        return new AdminStatsUsersFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
