package com.example.rmplus;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RequestPagerAdapter extends FragmentStateAdapter {

    boolean isAdmin;

    public RequestPagerAdapter(@NonNull FragmentActivity fa,
                               boolean isAdmin) {
        super(fa);
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        // ⭐ ADMIN SIDE → ORIGINAL FRAGMENT
        if (isAdmin) {

            if (position == 0)
                return RequestListFragment
                        .newInstance("pending", true);

            if (position == 1)
                return RequestListFragment
                        .newInstance("accepted", true);

            return RequestListFragment
                    .newInstance("rejected", true);
        }

        // ⭐ USER SIDE → UNIFIED FRAGMENT
        else {

            if (position == 0)
                return MyRequestListFragment
                        .newInstance("pending");

            if (position == 1)
                return MyRequestListFragment
                        .newInstance("accepted");

            return MyRequestListFragment
                    .newInstance("rejected");
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
