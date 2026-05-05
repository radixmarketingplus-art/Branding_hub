package com.rmads.maker;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImagePreviewActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_preview);

        ImageView img = findViewById(R.id.fullImage);
        VideoView video = findViewById(R.id.fullVideo);
        ProgressBar progress = findViewById(R.id.loadingProgress);

        String path = getIntent().getStringExtra("img");
        if (path == null || path.isEmpty()) {
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Detect video by intent flag OR by URL extension
        boolean isVideo = getIntent().getBooleanExtra("is_video", false)
                || path.toLowerCase().contains(".mp4")
                || path.toLowerCase().contains(".mov")
                || path.toLowerCase().contains(".mkv")
                || path.toLowerCase().contains(".webm")
                || path.toLowerCase().contains(".3gp");

        if (isVideo) {
            // Hide image, show video player
            img.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            
            // ✅ Show progress bar while video is buffering
            if (progress != null) progress.setVisibility(View.VISIBLE);

            video.setVideoURI(Uri.parse(path));

            MediaController mc = new MediaController(this);
            mc.setAnchorView(video);
            video.setMediaController(mc);
            
            video.setOnPreparedListener(mp -> {
                // ✅ Hide progress bar when video is ready to play
                if (progress != null) progress.setVisibility(View.GONE);
                mp.start();
                
                // Set scale to fit center if needed
                float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                float screenRatio = video.getWidth() / (float) video.getHeight();
                float scaleX = videoRatio / screenRatio;
                if (scaleX >= 1f) {
                    video.setScaleX(scaleX);
                } else {
                    video.setScaleY(1f / scaleX);
                }
            });

            video.setOnErrorListener((mp, what, extra) -> {
                if (progress != null) progress.setVisibility(View.GONE);
                Toast.makeText(this, "Could not play video. The file might be too large or buffering.", Toast.LENGTH_SHORT).show();
                return true;
            });

        } else {
            // Show image, hide video player
            video.setVisibility(View.GONE);
            img.setVisibility(View.VISIBLE);
            if (progress != null) progress.setVisibility(View.GONE);

            Glide.with(this)
                    .load(path)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(img);
        }
    }
}
