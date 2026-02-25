package com.example.rmplus;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminContactRequestsActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_contact_requests);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        RequestPagerAdapter adapter =
                new RequestPagerAdapter(this, true);

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(
                tabLayout,
                viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText(R.string.tab_pending);
                    else if (position == 1) tab.setText(R.string.tab_accepted);
                    else tab.setText(R.string.tab_rejected);
                }
        ).attach();

        // 🔥 Apply text styling logic
        setupTabTextBehavior();
    }

    /* ---------------- TAB TEXT SIZE + BOLD LOGIC ---------------- */

    private void setupTabTextBehavior() {

        // initial state
        updateTabStyles(tabLayout.getSelectedTabPosition());

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        updateTabStyles(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        // handled centrally
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {}
                }
        );
    }

    private void updateTabStyles(int selectedPos) {

        for (int i = 0; i < tabLayout.getTabCount(); i++) {

            View tabView = tabLayout.getTabAt(i).view;

            // TabLayout internal TextView
            for (int j = 0; j < ((android.view.ViewGroup) tabView).getChildCount(); j++) {

                View child = ((android.view.ViewGroup) tabView).getChildAt(j);

                if (child instanceof android.widget.TextView) {

                    android.widget.TextView tv =
                            (android.widget.TextView) child;

                    tv.setTypeface(null, android.graphics.Typeface.BOLD);

                    if (i == selectedPos) {
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
                    } else {
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                    }
                }
            }
        }
    }
}