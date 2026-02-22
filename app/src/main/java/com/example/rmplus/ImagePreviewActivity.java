package com.example.rmplus;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_preview);

        ImageView img = findViewById(R.id.fullImage);

        String url = getIntent().getStringExtra("img");

        img.setImageURI(android.net.Uri.parse(url));
    }
}
