package com.example.rmplus;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdRequestPagerAdapter
        extends FragmentStateAdapter {

    boolean isAdmin;

    public AdRequestPagerAdapter(
            @NonNull FragmentActivity fa,
            boolean isAdmin) {
        super(fa);
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        if(position==0)
            return AdRequestListFragment
                    .newInstance("pending",isAdmin);

        if(position==1)
            return AdRequestListFragment
                    .newInstance("accepted",isAdmin);

        return AdRequestListFragment
                .newInstance("rejected",isAdmin);
    }

    public int getItemCount(){ return 3; }
}
