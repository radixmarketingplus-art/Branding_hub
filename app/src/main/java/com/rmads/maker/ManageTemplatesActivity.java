package com.rmads.maker;

import android.app.AlertDialog;
import java.util.ArrayList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;
import java.io.File;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ManageTemplatesActivity extends BaseActivity {

    FrameLayout canvas;
    View activeOverlay;
    View activeContent;
    ImageView imgTemplate;
    VideoView videoTemplate;
    com.google.android.material.card.MaterialCardView canvasContainer;
    com.google.android.material.button.MaterialButton btnSave;
    com.google.android.material.button.MaterialButton btnSendBack, btnBringFront, btnTextSendBack, btnTextBringFront;
    com.google.android.material.button.MaterialButton btnFont, btnBold, btnItalic, btnUnderline;
    Button btnColor;
    LinearLayout btnText, btnGallery, btnFrame, btnBgColorTool;
    LinearLayout textLayerControlsLayout, imageLayerControlsLayout;
    TextView btnTabNormalFrame, btnTabBusinessFrame;
    com.google.android.material.card.MaterialCardView textControls, imageControls, frameControls;
    FrameLayout dynamicFrameContainer;
    RecyclerView rvFrames;
    String uName, uMobile, uEmail, uProfileUrl;

    static final int PICK_IMAGE = 101;

    String originalPath;
    String templateId;
    String uid;
    boolean isBusinessFrame = false;
    boolean isVideo = false;
    DatabaseReference rootRef;

    int selectedFrameLayoutId = -1;
    String selectedFrameUrl = null;
    int canvasBgColor = Color.TRANSPARENT;

    ScaleGestureDetector scaleGestureDetector;
    float lastPanX, lastPanY;
    boolean isPanningCanvas = false;
    boolean isTransformingOverlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_templates);

        // ==== Bind ====
        canvas = findViewById(R.id.canvas);
        canvas.setClipChildren(false);
        canvas.setClipToPadding(false);
        imgTemplate = findViewById(R.id.imgTemplate);
        videoTemplate = findViewById(R.id.videoTemplate);
        canvasContainer = findViewById(R.id.canvasContainer);
        btnText = findViewById(R.id.btnText);
        btnGallery = findViewById(R.id.btnGallery);
        btnSave = findViewById(R.id.btnSave);
        btnColor = findViewById(R.id.btnColor);
        textControls = findViewById(R.id.textControls);
        imageControls = findViewById(R.id.imageControls);
        imageControls = findViewById(R.id.imageControls);
        btnFrame = findViewById(R.id.btnFrame);
        frameControls = findViewById(R.id.frameControls);
        rvFrames = findViewById(R.id.rvFrames);
        dynamicFrameContainer = findViewById(R.id.dynamicFrameContainer);
        btnTabNormalFrame = findViewById(R.id.btnTabNormalFrame);
        btnTabBusinessFrame = findViewById(R.id.btnTabBusinessFrame);
        btnBgColorTool = findViewById(R.id.btnBgColorTool);
        btnSendBack = findViewById(R.id.btnSendBack);
        btnBringFront = findViewById(R.id.btnBringFront);
        btnTextSendBack = findViewById(R.id.btnTextSendBack);
        btnTextBringFront = findViewById(R.id.btnTextBringFront);
        imageLayerControlsLayout = findViewById(R.id.imageLayerControlsLayout);
        findViewById(R.id.btnDeleteText).setOnClickListener(v -> deleteActiveOverlay());
        findViewById(R.id.btnDeleteImage).setOnClickListener(v -> deleteActiveOverlay());
        isTransformingOverlay = false;

        btnFont = findViewById(R.id.btnFont);
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);

        uid = FirebaseAuth.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // ==== Load image ====
        originalPath = getIntent().getStringExtra("uri");
        templateId = getIntent().getStringExtra("id"); // ✅ Try to get the real Firebase ID first

        if (originalPath != null) {
            if (templateId == null || templateId.isEmpty()) {
                templateId = makeSafeKey(originalPath); // fallback
            }

            isVideo = getIntent().getBooleanExtra("isVideo", false);
            if (isVideo) {
                // 🎬 Adjust Ratio for Reel/Video (9:16)
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) canvasContainer
                        .getLayoutParams();
                lp.dimensionRatio = "9:16";
                canvasContainer.setLayoutParams(lp);

                imgTemplate.setVisibility(View.GONE);
                videoTemplate.setVisibility(View.VISIBLE);
                videoTemplate.setVideoURI(Uri.parse(originalPath));

                android.widget.MediaController mediaController = new android.widget.MediaController(this);
                mediaController.setAnchorView(videoTemplate);
                videoTemplate.setMediaController(mediaController);

                videoTemplate.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    videoTemplate.start();
                });

                // Hide Business Frame tab for videos as requested
                btnTabBusinessFrame.setVisibility(View.GONE);
            } else {
                // 📸 1:1 for Business/Normal Frames
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) canvasContainer
                        .getLayoutParams();
                lp.dimensionRatio = "1:1";
                canvasContainer.setLayoutParams(lp);

                loadImageSmart(originalPath, imgTemplate);
            }
        }

        // LOAD SAVED STATE IF ANY
        loadSavedCanvasState();

        String category = getIntent().getStringExtra("category");
        isBusinessFrame = (category != null && category.contains("Business Frame"));

        if (isBusinessFrame) {
            btnFrame.setVisibility(View.GONE);
        } else {
            // Hide bg color tool for non-business frames
            btnBgColorTool.setVisibility(View.GONE);
        }

        // Tap on canvas to deselect all, and also Handle 1-finger Pan on Canvas!
        canvas.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        deselectAll();
                        return true; // We MUST return true to receive ACTION_MOVE
                    } else if (action == MotionEvent.ACTION_MOVE && canvas.getScaleX() > 1.0f) {
                        float dx = event.getRawX() - lastX;
                        float dy = event.getRawY() - lastY;

                        canvas.setTranslationX(canvas.getTranslationX() + dx);
                        canvas.setTranslationY(canvas.getTranslationY() + dy);

                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        return true;
                    }
                }
                return false;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // ==== FETCH USER DATA FOR DYNAMIC FRAMES ====
        loadUserData();

        // ==== CANVAS PINCH TO ZOOM & PAN ====
        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (canvas == null)
                            return false;
                        float scaleFactor = canvas.getScaleX() * detector.getScaleFactor();
                        scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f)); // Limit zoom (1.0x to 5x)
                        canvas.setScaleX(scaleFactor);
                        canvas.setScaleY(scaleFactor);

                        // Snap back translation if fully zoomed out
                        if (scaleFactor <= 1.0f) {
                            canvas.setTranslationX(0f);
                            canvas.setTranslationY(0f);
                        }

                        return true;
                    }
                });

        // ==== FRAME DESIGN SELECTION ====
        btnFrame.setOnClickListener(v -> {
            textControls.setVisibility(View.GONE);
            imageControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.VISIBLE);
            btnTabNormalFrame.performClick(); // default to normal frame
        });

        btnTabNormalFrame.setOnClickListener(v -> {
            btnTabNormalFrame.setTypeface(null, android.graphics.Typeface.BOLD);
            btnTabNormalFrame.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface));
            btnTabNormalFrame.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            btnTabBusinessFrame.setTypeface(null, android.graphics.Typeface.NORMAL);
            btnTabBusinessFrame.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            btnTabBusinessFrame.setBackgroundResource(R.drawable.bg_filter_chip);
            setupFrameAdapter("Normal");
        });

        btnTabBusinessFrame.setOnClickListener(v -> {
            btnTabBusinessFrame.setTypeface(null, android.graphics.Typeface.BOLD);
            btnTabBusinessFrame.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface));
            btnTabBusinessFrame.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            btnTabNormalFrame.setTypeface(null, android.graphics.Typeface.NORMAL);
            btnTabNormalFrame.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            btnTabNormalFrame.setBackgroundResource(R.drawable.bg_filter_chip);
            setupFrameAdapter("Business");
        });

        // ==== TEXT ====
        btnText.setOnClickListener(v -> {
            addNewText();
        });

        // ==== GALLERY (IMAGE) ====
        btnGallery.setOnClickListener(v -> {
            pickImageFromGallery();
        });

        // ==== BG COLOR ====
        btnBgColorTool.setOnClickListener(v -> {
            deselectAll();
            showColorPickerForCanvas();
        });

        // ==== LAYER CONTROLS (IMAGE) ====
        btnSendBack.setOnClickListener(v -> {
            if (activeOverlay != null && activeOverlay.getParent() == canvas) {
                canvas.removeView(activeOverlay);
                int targetIndex = isVideo ? 2 : (isBusinessFrame ? 0 : 1);
                canvas.addView(activeOverlay, targetIndex);
            }
        });

        btnBringFront.setOnClickListener(v -> {
            if (activeOverlay != null && activeOverlay.getParent() == canvas) {
                activeOverlay.bringToFront();
            }
        });

        // ==== LAYER CONTROLS (TEXT) ====
        btnTextSendBack.setOnClickListener(v -> {
            if (activeOverlay != null && activeOverlay.getParent() == canvas) {
                canvas.removeView(activeOverlay);
                int targetIndex = isVideo ? 2 : (isBusinessFrame ? 0 : 1);
                canvas.addView(activeOverlay, targetIndex);
            }
        });

        btnTextBringFront.setOnClickListener(v -> {
            if (activeOverlay != null && activeOverlay.getParent() == canvas) {
                activeOverlay.bringToFront();
            }
        });

        // ==== TEXT FORMATTING CONTROLS ====
        btnBold.setOnClickListener(v -> toggleTextStyle(android.graphics.Typeface.BOLD));
        btnItalic.setOnClickListener(v -> toggleTextStyle(android.graphics.Typeface.ITALIC));
        btnUnderline.setOnClickListener(v -> toggleUnderline());
        btnFont.setOnClickListener(v -> showFontPicker());

        // ==== SEEKBARS ====
        // SeekBars removed, now using gestures

        // ==== COLOR ====
        btnColor.setOnClickListener(v -> {
            if (activeContent instanceof TextView) {
                new AmbilWarnaDialog(
                        this,
                        ((TextView) activeContent).getCurrentTextColor(),
                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                ((TextView) activeContent).setTextColor(color);
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {
                            }
                        }).show();
            }
        });

        // Text size & Logo size seekbars removed

        // ==== SAVE ====
        btnSave.setOnClickListener(v -> checkSubscription(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_save_template)
                    .setMessage(R.string.msg_save_confirm)
                    .setPositiveButton(R.string.btn_yes,
                            (d, w) -> saveFinalImage())
                    .setNegativeButton(R.string.btn_no, null)
                    .show();
        }));

    }

    // ================= TEXT EDIT =================
    private void deleteActiveOverlay() {
        if (activeOverlay != null) {
            View toRemove = activeOverlay;
            deselectAll();
            canvas.removeView(toRemove);
        }
    }

    // ================= PICK IMAGE =================
    void pickImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == PICK_IMAGE && res == RESULT_OK && data != null) {
            startCrop(data.getData());
        }

        if (rc == UCrop.REQUEST_CROP && res == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                addNewImage(resultUri);
            }
        }
    }

    private void startCrop(Uri uri) {
        String destinationFileName = "RMAdsMaker_Crop_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withOptions(new UCrop.Options());
        uCrop.start(this);
    }

    private void loadImageSmart(String path, ImageView imageView) {
        if (path == null || path.isEmpty())
            return;

        // 🌐 LOAD FROM VPS URL OR LOCAL FILE (Glide handles both)
        Glide.with(this)
                .load(path)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    // ================= DYNAMIC OVERLAYS =================

    private void addNewText() {
        TextView tv = new TextView(this);
        tv.setText(R.string.ph_edit_text);
        tv.setTextSize(24);
        tv.setTextColor(Color.BLACK);
        tv.setPadding(0, 0, 0, 0); // No internal padding for tight fit
        addOverlay(tv, false);
    }

    private void addNewImage(Uri uri) {
        com.google.android.material.imageview.ShapeableImageView iv = new com.google.android.material.imageview.ShapeableImageView(
                this);
        iv.setImageURI(uri);
        iv.setAdjustViewBounds(true);
        iv.setPadding(0, 0, 0, 0); // No internal padding for tight fit
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Store path for state recovery
        iv.setTag(R.id.tag_image_path, uri.toString());

        // Call addOverlay without inner click listener tracking inside addNewImage
        addOverlay(iv, true);
    }

    private void applyShape(com.google.android.material.imageview.ShapeableImageView iv, int shape) {
        com.google.android.material.shape.ShapeAppearanceModel.Builder builder = new com.google.android.material.shape.ShapeAppearanceModel.Builder();
        if (shape == 0) { // Rect
            builder.setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 0f);
        } else if (shape == 1) { // Circle
            builder.setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
                    .setAllCornerSizes(new com.google.android.material.shape.RelativeCornerSize(0.5f));
        }
        iv.setShapeAppearanceModel(builder.build());
    }

    private void addOverlay(View content, boolean isImage) {
        deselectAll();

        TransformableOverlay overlay = new TransformableOverlay(this);
        overlay.setContent(content);
        
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        overlay.setLayoutParams(lp);
        
        overlay.setX(200);
        overlay.setY(200);
        
        overlay.setOnOverlayClickListener(o -> setActive(o));
        
        canvas.addView(overlay);
        setActive(overlay);

        content.setOnClickListener(v -> {
            if (activeOverlay == overlay) {
                if (!isImage) {
                    showTextEditor((TextView) content);
                } else {
                    Object tag = content.getTag();
                    int currentShape = tag instanceof Integer ? (Integer) tag : 0;
                    int nextShape = (currentShape + 1) % 2; 
                    content.setTag(nextShape);
                    applyShape((com.google.android.material.imageview.ShapeableImageView) content, nextShape);
                }
            } else {
                setActive(overlay);
            }
        });
    }

    private void showColorPickerForCanvas() {
        int defaultColor = getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant);
        if (canvas.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
            defaultColor = ((android.graphics.drawable.ColorDrawable) canvas.getBackground()).getColor();
        }

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                canvas.setBackgroundColor(color);
                canvasBgColor = color;
            }
        });
        dialog.show();
    }

    private void setActive(View view) {
        deselectAll();
        if (!(view instanceof TransformableOverlay)) return;
        
        activeOverlay = view;
        TransformableOverlay overlay = (TransformableOverlay) view;
        overlay.setSelected(true);
        activeContent = overlay.getContent();

        if (activeContent instanceof TextView) {
            textControls.setVisibility(View.VISIBLE);
            imageControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);

            TextView tv = (TextView) activeContent;
            int style = tv.getTypeface() != null ? tv.getTypeface().getStyle() : android.graphics.Typeface.NORMAL;
            btnBold.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    (style & android.graphics.Typeface.BOLD) != 0
                            ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                            : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
            btnItalic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    (style & android.graphics.Typeface.ITALIC) != 0
                            ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                            : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
            boolean isUnderlined = (tv.getPaintFlags() & android.graphics.Paint.UNDERLINE_TEXT_FLAG) != 0;
            btnUnderline.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isUnderlined ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                            : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
        } else {
            imageControls.setVisibility(View.VISIBLE);
            textControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);
        }
    }

    private void deselectAll() {
        if (canvas == null) return;
        for (int i = 0; i < canvas.getChildCount(); i++) {
            View child = canvas.getChildAt(i);
            if (child instanceof TransformableOverlay) {
                ((TransformableOverlay) child).setSelected(false);
            }
        }
        activeOverlay = null;
        activeContent = null;
        if (textControls != null) textControls.setVisibility(View.GONE);
        if (imageControls != null) imageControls.setVisibility(View.GONE);
        if (frameControls != null) frameControls.setVisibility(View.GONE);
        isTransformingOverlay = false;
    }


    void showTextEditor(TextView target) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        EditText input = new EditText(this);
        input.setText(target.getText());
        b.setTitle(R.string.ph_edit_text);
        b.setView(input);
        b.setPositiveButton(R.string.btn_apply,
                (d, w) -> target.setText(input.getText()));
        b.show();
    }

    // ================= SAVE IMAGE =================
    void saveFinalImage() {
        checkDownloadLimit(() -> {
            if (isVideo) {
                saveFinalVideo();
                return;
            }

            deselectAll();

            canvas.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(canvas.getDrawingCache());
            canvas.setDrawingCacheEnabled(false);
            String savedPath = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "RMAdsMaker_Edit_" + System.currentTimeMillis(),
                    getString(R.string.desc_edited_template));

            if (savedPath != null) {
                Toast.makeText(this, R.string.msg_saved_to_gallery, Toast.LENGTH_SHORT).show();
                incrementDownloadCount(); // Track the save

                long now = System.currentTimeMillis();
                
                // Log as Save only (Final format)
                rootRef.child("template_activity").child(templateId).child("saves").child(uid).setValue(now);
                rootRef.child("user_activity").child(uid).child("saves").child(templateId).setValue(savedPath);
        } else {
            Toast.makeText(this, R.string.msg_failed_save_image, Toast.LENGTH_SHORT).show();
        }
    });
}

    private void saveFinalVideo() {
        deselectAll();

        // Custom Progress Dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 40);

        TextView tv = new TextView(this);
        tv.setText(R.string.msg_generating_video);
        tv.setTextSize(16);
        tv.setPadding(0, 0, 0, 30);
        layout.addView(tv);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setIndeterminate(false);
        pb.setMax(100);
        pb.setProgress(0);
        pb.setScaleY(3f); // Make it thicker to be clearly visible
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            pb.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4285F4"))); // Google
                                                                                                               // Blue
            pb.setProgressBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
        }
        layout.addView(pb);

        TextView progressText = new TextView(this);
        progressText.setText(getString(R.string.format_percentage, 0));
        progressText.setGravity(android.view.Gravity.END);
        progressText.setPadding(0, 10, 0, 0);
        layout.addView(progressText);

        builder.setView(layout);
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        int totalDurationMs = videoTemplate.getDuration();
        if (totalDurationMs <= 0)
            totalDurationMs = 1; // safeguard
        final int finalDuration = totalDurationMs;

        com.arthenica.ffmpegkit.FFmpegKitConfig.enableStatisticsCallback(newStatistics -> {
            int time = (int) newStatistics.getTime();
            int progress = (int) ((time * 100) / finalDuration);
            if (progress > 100)
                progress = 100;
            if (progress < 0)
                progress = 0;

            final int p = progress;
            runOnUiThread(() -> {
                pb.setProgress(p);
                progressText.setText(getString(R.string.format_percentage, p));
            });
        });

        // 1. Hide video and image to capture pure overlays with transparent background
        videoTemplate.setVisibility(View.INVISIBLE);
        int oldImgVis = imgTemplate.getVisibility();
        imgTemplate.setVisibility(View.INVISIBLE);

        android.graphics.drawable.Drawable oldBg = canvas.getBackground();
        canvas.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        canvas.post(() -> {
            canvas.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(canvas.getDrawingCache());
            canvas.setDrawingCacheEnabled(false);

            // Restore UI
            canvas.setBackground(oldBg);
            videoTemplate.setVisibility(View.VISIBLE);
            imgTemplate.setVisibility(oldImgVis);

            // 2. Save overlay to temp file
            java.io.File overlayFile = new java.io.File(getCacheDir(), "temp_overlay.png");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(overlayFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                dialog.dismiss();
                Toast.makeText(this, R.string.msg_failed_capture_overlay, Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Define output file
            java.io.File moviesDir = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES);
            if (!moviesDir.exists())
                moviesDir.mkdirs();
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(new java.util.Date());
            java.io.File outputFile = new java.io.File(moviesDir, "RMAdsMaker_Video_" + timestamp + ".mp4");

            // 4. Build ffmpeg command
            int canvasW = bitmap.getWidth();
            int canvasH = bitmap.getHeight();
            if (canvasW % 2 != 0)
                canvasW += 1; // ffmpeg encoders often complain about odd dimensions
            if (canvasH % 2 != 0)
                canvasH += 1;

            String filterComplex = String.format(java.util.Locale.US,
                    "[0:v]scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2[vidpadded];[vidpadded][1:v]overlay=0:0",
                    canvasW, canvasH, canvasW, canvasH);

            // Use -b:v 15M to force high bitrate and maintain original quality, -q:v 2 as
            // fallback for quality scale
            String command = String.format(java.util.Locale.US,
                    "-y -i \"%s\" -i \"%s\" -filter_complex \"%s\" -b:v 15M -q:v 2 -c:a copy \"%s\"",
                    originalPath, overlayFile.getAbsolutePath(), filterComplex, outputFile.getAbsolutePath());

            // 5. Execute
            com.arthenica.ffmpegkit.FFmpegKit.executeAsync(command, session -> {
                com.arthenica.ffmpegkit.ReturnCode returnCode = session.getReturnCode();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    com.arthenica.ffmpegkit.FFmpegKitConfig.enableStatisticsCallback(null); // clear callback
                    if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(returnCode)) {
                        Toast.makeText(ManageTemplatesActivity.this, R.string.msg_video_saved_success,
                                Toast.LENGTH_LONG).show();

                        android.content.Intent mediaScanIntent = new android.content.Intent(
                                android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        android.net.Uri contentUri = android.net.Uri.fromFile(outputFile);
                        mediaScanIntent.setData(contentUri);
                        long now = System.currentTimeMillis();
                        String vPath = outputFile.getAbsolutePath();
                        // Log as Save only
                        rootRef.child("template_activity").child(templateId).child("saves").child(uid).setValue(now);
                        rootRef.child("user_activity").child(uid).child("saves").child(templateId).setValue(vPath);
                        
                        incrementDownloadCount(); // Track video edit save
                    } else {
                        Toast.makeText(ManageTemplatesActivity.this,
                                getString(R.string.msg_failed_save_video_error, returnCode.getValue()), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    private void toggleTextStyle(int style) {
        if (activeContent instanceof TextView) {
            TextView tv = (TextView) activeContent;
            android.graphics.Typeface current = tv.getTypeface();
            int currentStyle = (current != null) ? current.getStyle() : android.graphics.Typeface.NORMAL;
            int newStyle = currentStyle ^ style;

            android.graphics.Typeface tf = android.graphics.Typeface.create(current, newStyle);
            tv.setTypeface(tf);

            // Sync toggle button highlight
            if (style == android.graphics.Typeface.BOLD) {
                btnBold.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        (newStyle & android.graphics.Typeface.BOLD) != 0
                                ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                                : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
            } else {
                btnItalic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        (newStyle & android.graphics.Typeface.ITALIC) != 0
                                ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                                : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
            }
        }
    }

    private void toggleUnderline() {
        if (activeContent instanceof TextView) {
            TextView tv = (TextView) activeContent;
            int flags = tv.getPaintFlags();
            boolean isUnderlined;
            if ((flags & android.graphics.Paint.UNDERLINE_TEXT_FLAG) != 0) {
                tv.setPaintFlags(flags & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                isUnderlined = false;
            } else {
                tv.setPaintFlags(flags | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                isUnderlined = true;
            }
            btnUnderline.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isUnderlined ? getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                            : getColorFromAttr(com.google.android.material.R.attr.colorSurfaceVariant)));
        }
    }

    private void showFontPicker() {
        if (!(activeContent instanceof TextView))
            return;
        TextView tv = (TextView) activeContent;

        String[] fontNames = {
                getString(R.string.font_default_sans), getString(R.string.font_serif), getString(R.string.font_monospace),
                getString(R.string.font_cursive), getString(R.string.font_handwriting), getString(R.string.font_heavy),
                getString(R.string.font_display), getString(R.string.font_bebas), getString(R.string.font_medium), getString(R.string.font_black)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_choose_font)
                .setItems(fontNames, (dialog, which) -> {
                    android.graphics.Typeface tf = null;
                    try {
                        switch (which) {
                            case 0:
                                tf = android.graphics.Typeface.SANS_SERIF;
                                break;
                            case 1:
                                tf = android.graphics.Typeface.SERIF;
                                break;
                            case 2:
                                tf = android.graphics.Typeface.MONOSPACE;
                                break;
                            case 3:
                                tf = android.graphics.Typeface.createFromAsset(getAssets(),
                                        "fonts/Lobster-Regular.ttf");
                                break;
                            case 4:
                                tf = android.graphics.Typeface.createFromAsset(getAssets(),
                                        "fonts/Pacifico-Regular.ttf");
                                break;
                            case 5:
                                tf = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Anton-Regular.ttf");
                                break;
                            case 6:
                                tf = android.graphics.Typeface.createFromAsset(getAssets(),
                                        "fonts/Righteous-Regular.ttf");
                                break;
                            case 7:
                                tf = android.graphics.Typeface.createFromAsset(getAssets(),
                                        "fonts/BebasNeue-Regular.ttf");
                                break;
                            case 8:
                                tf = android.graphics.Typeface.create("sans-serif-medium",
                                        android.graphics.Typeface.NORMAL);
                                break;
                            case 9:
                                tf = android.graphics.Typeface.create("sans-serif-black",
                                        android.graphics.Typeface.NORMAL);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        tf = android.graphics.Typeface.DEFAULT;
                    }

                    if (tf != null) {
                        int currentStyle = tv.getTypeface() != null ? tv.getTypeface().getStyle()
                                : android.graphics.Typeface.NORMAL;
                        tv.setTypeface(tf, currentStyle);
                    }
                })
                .show();
    }

    private void loadUserData() {
        if (uid == null)
            return;
        rootRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                uName = s.child("name").getValue(String.class);
                uMobile = s.child("mobile").getValue(String.class);
                uEmail = s.child("email").getValue(String.class);
                uProfileUrl = s.child("profileImage").getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    static class FrameModel {
        String title;
        int layoutId;
        String imageUrl;
        int fallbackIcon;

        FrameModel(String t, int l, String i, int f) {
            title = t;
            layoutId = l;
            imageUrl = i;
            fallbackIcon = f;
        }
    }

    private void setupFrameAdapter(String type) {
        ArrayList<FrameModel> designs = new ArrayList<>();
        designs.add(new FrameModel(getString(R.string.label_none), -1, null, R.drawable.ic_close_gray));

        if ("Normal".equals(type)) {
            designs.add(new FrameModel(getString(R.string.label_design_format, 1), 1, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 2), 2, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 3), 3, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 4), 4, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 5), 5, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 6), 6, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 7), 7, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 8), 8, null, R.drawable.ic_edit));
            designs.add(new FrameModel(getString(R.string.label_design_format, 9), 9, null, R.drawable.ic_edit));
            renderFrameAdapter(designs);
        } else {
            rootRef.child("templates").child("Business Frame")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            int index = 1;
                            for (DataSnapshot subSnapshot : s.getChildren()) {
                                for (DataSnapshot item : subSnapshot.getChildren()) {
                                    String url = item.hasChild("imagePath")
                                            ? item.child("imagePath").getValue(String.class)
                                            : item.child("url").getValue(String.class);
                                    if (url != null) {
                                        designs.add(new FrameModel(getString(R.string.label_business_format, index), 0,
                                                url, R.drawable.ic_edit));
                                        index++;
                                    }
                                }
                            }
                            renderFrameAdapter(designs);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                        }
                    });
        }
    }

    private void renderFrameAdapter(ArrayList<FrameModel> designs) {
        rvFrames.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvFrames.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_frame_design, parent, false);
                return new RecyclerView.ViewHolder(v) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView t = holder.itemView.findViewById(R.id.txtTitle);
                ImageView i = holder.itemView.findViewById(R.id.imgIcon);
                FrameModel item = designs.get(position);

                if (item.imageUrl != null) {
                    t.setVisibility(View.GONE);
                    i.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ((LinearLayout) i.getParent()).setPadding(0, 0, 0, 0);

                    i.setImageTintList(null); // remove tint
                    Glide.with(ManageTemplatesActivity.this).load(item.imageUrl).into(i);
                } else if (item.layoutId != -1) {
                    t.setVisibility(View.GONE);
                    i.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ((LinearLayout) i.getParent()).setPadding(0, 0, 0, 0);

                    i.setImageTintList(null); // remove tint

                    Bitmap previewBmp = createFramePreview(item.layoutId);
                    i.setImageBitmap(previewBmp);
                } else {
                    t.setVisibility(View.VISIBLE);
                    t.setText(item.title);

                    int size40 = (int) (40 * getResources().getDisplayMetrics().density);
                    i.setLayoutParams(new LinearLayout.LayoutParams(size40, size40));
                    i.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    int pad8 = (int) (8 * getResources().getDisplayMetrics().density);
                    ((LinearLayout) i.getParent()).setPadding(pad8, pad8, pad8, pad8);

                    i.setImageResource(item.fallbackIcon);
                    i.setImageTintList(android.content.res.ColorStateList
                            .valueOf(getColorFromAttr(com.google.android.material.R.attr.colorPrimary)));
                }

                holder.itemView.setOnClickListener(v -> {
                    dynamicFrameContainer.removeAllViews();
                    selectedFrameLayoutId = item.layoutId;
                    selectedFrameUrl = item.imageUrl;

                    if (item.layoutId != -1) {
                        if (item.imageUrl != null) {
                            // Overlay image frame
                            ImageView iv = new ImageView(ManageTemplatesActivity.this);
                            iv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            Glide.with(ManageTemplatesActivity.this).load(item.imageUrl).into(iv);
                            dynamicFrameContainer.addView(iv);
                        } else {
                            applyDynamicFrame(item.layoutId);
                        }
                    }
                });
            }

            @Override
            public int getItemCount() {
                return designs.size();
            }
        });
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private Bitmap createFramePreview(int designNum) {
        int layoutId = R.layout.frame_design_1;
        if (designNum == 2)
            layoutId = R.layout.frame_design_2;
        if (designNum == 3)
            layoutId = R.layout.frame_design_3;
        if (designNum == 4)
            layoutId = R.layout.frame_design_4;
        if (designNum == 5)
            layoutId = R.layout.frame_design_5;
        if (designNum == 6)
            layoutId = R.layout.frame_design_6;
        if (designNum == 7)
            layoutId = R.layout.frame_design_7;
        if (designNum == 8)
            layoutId = R.layout.frame_design_8;
        if (designNum == 9)
            layoutId = R.layout.frame_design_9;

        View frameView = getLayoutInflater().inflate(layoutId, null, false);

        TextView tName = frameView.findViewById(R.id.frame_name);
        TextView tMobile = frameView.findViewById(R.id.frame_mobile);
        TextView tEmail = frameView.findViewById(R.id.frame_email);
        ImageView iLogo = frameView.findViewById(R.id.frame_logo);

        if (uName != null && !uName.isEmpty())
            tName.setText(uName);
        else if (tName != null)
            tName.setVisibility(View.GONE);

        if (uMobile != null && !uMobile.isEmpty())
            tMobile.setText(uMobile);
        else if (tMobile != null)
            tMobile.setVisibility(View.GONE);

        if (uEmail != null && !uEmail.isEmpty())
            tEmail.setText(uEmail);
        else if (tEmail != null)
            tEmail.setVisibility(View.GONE);

        if (iLogo != null) {
            iLogo.setVisibility(View.GONE);
        }

        int sizePx = 1080;
        int measureSpec = View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY);
        frameView.measure(measureSpec, measureSpec);
        frameView.layout(0, 0, sizePx, sizePx);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bitmap);

        c.drawColor(android.graphics.Color.parseColor("#E0E0E0"));

        frameView.draw(c);

        return bitmap;
    }

    private void applyDynamicFrame(int designNum) {
        dynamicFrameContainer.removeAllViews();
        int layoutId = R.layout.frame_design_1;
        if (designNum == 2)
            layoutId = R.layout.frame_design_2;
        if (designNum == 3)
            layoutId = R.layout.frame_design_3;
        if (designNum == 4)
            layoutId = R.layout.frame_design_4;
        if (designNum == 5)
            layoutId = R.layout.frame_design_5;
        if (designNum == 6)
            layoutId = R.layout.frame_design_6;
        if (designNum == 7)
            layoutId = R.layout.frame_design_7;
        if (designNum == 8)
            layoutId = R.layout.frame_design_8;
        if (designNum == 9)
            layoutId = R.layout.frame_design_9;

        View frameView = getLayoutInflater().inflate(layoutId, dynamicFrameContainer, false);

        TextView tName = frameView.findViewById(R.id.frame_name);
        TextView tMobile = frameView.findViewById(R.id.frame_mobile);
        TextView tEmail = frameView.findViewById(R.id.frame_email);
        ImageView iLogo = frameView.findViewById(R.id.frame_logo);

        if (uName != null && !uName.isEmpty())
            tName.setText(uName);
        else
            tName.setVisibility(View.GONE);
        if (uMobile != null && !uMobile.isEmpty())
            tMobile.setText(uMobile);
        else
            tMobile.setVisibility(View.GONE);
        if (uEmail != null && !uEmail.isEmpty())
            tEmail.setText(uEmail);
        else
            tEmail.setVisibility(View.GONE);

        if (uProfileUrl != null && !uProfileUrl.isEmpty()) {
            iLogo.setVisibility(View.VISIBLE);
            Glide.with(this).load(uProfileUrl).into(iLogo);
        } else {
            iLogo.setVisibility(View.GONE);
        }

        dynamicFrameContainer.addView(frameView);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Only allow canvas zoom if we are NOT transforming an overlay
        if (scaleGestureDetector != null && !isTransformingOverlay) {
            scaleGestureDetector.onTouchEvent(ev);
        }

        int action = ev.getActionMasked();

        // Multi-touch Pan (e.g. 2 fingers)
        // CRITICAL FIX: Only intercept if we are NOT transforming an overlay!
        if (ev.getPointerCount() > 1 && !isTransformingOverlay) {
            isPanningCanvas = false; // Reset 1-finger pan
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                lastPanX = ev.getX(0);
                lastPanY = ev.getY(0);

                // Cancel touches for children (stickers)
                MotionEvent cancelEvent = MotionEvent.obtain(ev);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
            } else if (action == MotionEvent.ACTION_MOVE && canvas != null && canvas.getScaleX() > 1.0f) {
                float dx = ev.getX(0) - lastPanX;
                float dy = ev.getY(0) - lastPanY;
                canvas.setTranslationX(canvas.getTranslationX() + dx);
                canvas.setTranslationY(canvas.getTranslationY() + dy);
                lastPanX = ev.getX(0);
                lastPanY = ev.getY(0);
            }
            return true;
        }

        // 1-finger pan is now handled by canvas's setOnTouchListener!

        // Default routing to children
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isVideo && videoTemplate != null) {
            videoTemplate.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isVideo && videoTemplate != null) {
            videoTemplate.start();
        }
    }

    // ================= STATE PERSISTENCE =================

    private String serializeCanvas() {
        try {
            JSONObject root = new JSONObject();
            root.put("bgColor", canvasBgColor);
            root.put("frameId", selectedFrameLayoutId);
            root.put("frameUrl", selectedFrameUrl != null ? selectedFrameUrl : "");

            JSONArray arr = new JSONArray();
            int startIdx = 3; // 0:Img, 1:Vid, 2:DynamicContainer
            for (int i = startIdx; i < canvas.getChildCount(); i++) {
                View wrapper = canvas.getChildAt(i);
                if (!(wrapper instanceof FrameLayout)) continue;
                
                View content = ((FrameLayout) wrapper).getChildAt(0);
                JSONObject obj = new JSONObject();
                
                obj.put("x", wrapper.getX());
                obj.put("y", wrapper.getY());
                obj.put("rotation", wrapper.getRotation());
                obj.put("scaleX", wrapper.getScaleX());
                obj.put("scaleY", wrapper.getScaleY());
                
                if (content instanceof TextView) {
                    TextView tv = (TextView) content;
                    obj.put("type", "text");
                    obj.put("text", tv.getText().toString());
                    obj.put("color", tv.getCurrentTextColor());
                    obj.put("size", tv.getTextSize());
                    obj.put("style", tv.getTypeface() != null ? tv.getTypeface().getStyle() : 0);
                } else if (content instanceof ImageView) {
                    obj.put("type", "image");
                    String path = (String) content.getTag(R.id.tag_image_path);
                    obj.put("path", path);
                    Object shape = content.getTag(); // Current shape (rect/circle)
                    obj.put("shape", shape instanceof Integer ? (Integer) shape : 0);
                }
                arr.put(obj);
            }
            root.put("overlays", arr);
            return root.toString();
        } catch (Exception e) { e.printStackTrace(); }
        return "{}";
    }

    private void loadSavedCanvasState() {
        if (templateId == null || uid == null) return;
        
        rootRef.child("user_activity").child(uid).child("edits").child(templateId)
            .child("state").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot s) {
                    String json = s.getValue(String.class);
                    if (json != null && !json.isEmpty()) {
                        applyCanvasState(json);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError e) {}
            });
    }

    private void applyCanvasState(String json) {
        try {
            JSONObject root = new JSONObject(json);
            
            // 1. BG Color
            if (root.has("bgColor")) {
                canvasBgColor = root.getInt("bgColor");
                if (canvasBgColor != Color.TRANSPARENT) {
                    canvas.setBackgroundColor(canvasBgColor);
                }
            }
            
            // 2. Frame
            if (root.has("frameId")) {
                selectedFrameLayoutId = root.getInt("frameId");
                selectedFrameUrl = root.optString("frameUrl", "");
                if (selectedFrameLayoutId != -1) {
                    if (!selectedFrameUrl.isEmpty()) {
                        ImageView iv = new ImageView(this);
                        iv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        Glide.with(this).load(selectedFrameUrl).into(iv);
                        dynamicFrameContainer.addView(iv);
                    } else {
                        applyDynamicFrame(selectedFrameLayoutId);
                    }
                }
            }

            // 3. Overlays
            JSONArray arr = root.getJSONArray("overlays");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String type = obj.getString("type");
                
                if ("text".equals(type)) {
                    TextView tv = new TextView(this);
                    tv.setText(obj.getString("text"));
                    tv.setTextColor(obj.getInt("color"));
                    tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, (float) obj.getDouble("size"));
                    int style = obj.optInt("style", 0);
                    tv.setTypeface(android.graphics.Typeface.defaultFromStyle(style));
                    tv.setPadding(0, 0, 0, 0);
                    
                    addOverlay(tv, false);
                    View wrapper = activeOverlay;
                    wrapper.setX((float) obj.getDouble("x"));
                    wrapper.setY((float) obj.getDouble("y"));
                    wrapper.setRotation((float) obj.getDouble("rotation"));
                    wrapper.setScaleX((float) obj.getDouble("scaleX"));
                    wrapper.setScaleY((float) obj.getDouble("scaleY"));
                    
                } else if ("image".equals(type)) {
                    String path = obj.optString("path");
                    if (path == null || path.isEmpty()) continue;
                    
                    com.google.android.material.imageview.ShapeableImageView iv = new com.google.android.material.imageview.ShapeableImageView(this);
                    iv.setTag(R.id.tag_image_path, path);
                    iv.setAdjustViewBounds(true);
                    iv.setPadding(0, 0, 0, 0);
                    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    
                    Glide.with(this).load(path).into(iv);
                    
                    int shape = obj.optInt("shape", 0);
                    iv.setTag(shape);
                    applyShape(iv, shape);
                    
                    addOverlay(iv, true);
                    View wrapper = activeOverlay;
                    wrapper.setX((float) obj.getDouble("x"));
                    wrapper.setY((float) obj.getDouble("y"));
                    wrapper.setRotation((float) obj.getDouble("rotation"));
                    wrapper.setScaleX((float) obj.getDouble("scaleX"));
                    wrapper.setScaleY((float) obj.getDouble("scaleY"));
                }
            }
            deselectAll();
        } catch (Exception e) { e.printStackTrace(); }
    }
    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            showDraftDialog();
        } else {
            finish();
        }
    }

    private boolean hasUnsavedChanges() {
        int startIdx = 3; // 0:Img, 1:Vid, 2:DynamicContainer
        return canvas.getChildCount() > startIdx || canvasBgColor != Color.TRANSPARENT || selectedFrameLayoutId != -1;
    }

    private void showDraftDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_save_draft)
                .setMessage(R.string.msg_save_draft)
                .setPositiveButton(R.string.btn_yes, (d, w) -> saveAsDraftAndExit())
                .setNegativeButton(R.string.btn_no, (d, w) -> finish())
                .show();
    }

    private void saveAsDraftAndExit() {
        long now = System.currentTimeMillis();
        String state = serializeCanvas();
        
        // Log as edit in template stats
        rootRef.child("template_activity").child(templateId).child("edits").child(uid).setValue(now);
        
        java.util.Map<String, Object> editData = new java.util.HashMap<>();
        editData.put("url", originalPath); // Thumbnail for draft (base template)
        editData.put("state", state);
        editData.put("timestamp", now);
        
        rootRef.child("user_activity").child(uid).child("edits").child(templateId).setValue(editData)
            .addOnCompleteListener(task -> {
                finish();
            });
    }
}
