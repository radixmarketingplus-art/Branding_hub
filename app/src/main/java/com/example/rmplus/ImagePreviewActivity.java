package com.example.rmplus;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.MediaController;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_preview);

        ImageView img = findViewById(R.id.fullImage);
        VideoView video = findViewById(R.id.fullVideo); // Need to ensure it exists in layout or handle it

        String path = getIntent().getStringExtra("img");

        if (path == null || path.isEmpty()) return;

        boolean isVideo = path.toLowerCase().endsWith(".mp4") || 
                          path.toLowerCase().endsWith(".webm") || 
                          path.toLowerCase().endsWith(".mkv") || 
                          path.toLowerCase().endsWith(".mov");

        if (isVideo) {
            img.setVisibility(View.GONE);
            if (video != null) {
                video.setVisibility(View.VISIBLE);
                video.setVideoURI(Uri.parse(path));
                
                MediaController mc = new MediaController(this);
                mc.setAnchorView(video);
                video.setMediaController(mc);
                
                video.start();
            }
        } else {
            if (video != null) video.setVisibility(View.GONE);
            img.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                    .load(path)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(img);
        }
    }
}