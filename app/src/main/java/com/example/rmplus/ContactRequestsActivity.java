package com.example.rmplus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.*;

public class ContactRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_requests);

        TabLayout tab = findViewById(R.id.tabLayout);
        ViewPager2 pager = findViewById(R.id.viewPager);

        pager.setAdapter(new RequestPagerAdapter(this, true));

        // ✅ Smooth behavior, no reload
        pager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tab, pager, (t, p) -> {
            if (p == 0) t.setText("Pending");
            if (p == 1) t.setText("Accepted");
            if (p == 2) t.setText("Rejected");
        }).attach();

        // ✅ Same active color as other pages
        tab.setTabTextColors(
                getColorFromAttr(com.google.android.material.R.attr.colorOnSurface),
                android.graphics.Color.parseColor("#4A6CF7")
        );
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
