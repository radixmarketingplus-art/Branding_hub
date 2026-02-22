package com.example.rmplus;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminAdRequestPagerAdapter
        extends FragmentStateAdapter {

    boolean isAdmin;

    public AdminAdRequestPagerAdapter(
            @NonNull FragmentActivity fa,
            boolean isAdmin) {
        super(fa);
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        if (position == 0)
            return AdminAdRequestListFragment
                    .newInstance("pending", isAdmin);

        if (position == 1)
            return AdminAdRequestListFragment
                    .newInstance("accepted", isAdmin);

        return AdminAdRequestListFragment
                .newInstance("rejected", isAdmin);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
