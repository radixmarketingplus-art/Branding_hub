package com.rmads.maker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ServiceRequestPagerAdapter extends FragmentStateAdapter {

    private final boolean isAdmin;

    public ServiceRequestPagerAdapter(@NonNull FragmentActivity fa, boolean isAdmin) {
        super(fa);
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String status = "pending";
        if (position == 1) status = "accepted";
        if (position == 2) status = "rejected";

        return ServiceRequestListFragment.newInstance(status, isAdmin);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
