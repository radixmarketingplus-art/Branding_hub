package com.example.rmplus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.*;

public class MyRequestsActivity extends AppCompatActivity {

    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_requests);

        TabLayout tab=findViewById(R.id.tabLayout);
        ViewPager2 pager=findViewById(R.id.viewPager);

        pager.setAdapter(new RequestPagerAdapter(this,false));

        new TabLayoutMediator(tab,pager,(t,p)->{
            if(p==0)t.setText("Pending");
            if(p==1)t.setText("Accepted");
            if(p==2)t.setText("Rejected");
        }).attach();
    }
}
