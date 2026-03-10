package com.example.rmplus;

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

public class ManageTemplatesActivity extends AppCompatActivity {

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
    SeekBar seekTextSize, seekImageSize;
    SeekBar seekTextRotate, seekImageRotate;

    String originalPath;
    String templateId;
    String uid;
    boolean isBusinessFrame = false;
    boolean isVideo = false;
    DatabaseReference rootRef;

    ScaleGestureDetector scaleGestureDetector;
    float lastPanX, lastPanY;
    boolean isPanningCanvas = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_templates);

        // ==== Bind ====
        canvas = findViewById(R.id.canvas);
        imgTemplate = findViewById(R.id.imgTemplate);
        videoTemplate = findViewById(R.id.videoTemplate);
        canvasContainer = findViewById(R.id.canvasContainer);
        btnText = findViewById(R.id.btnText);
        btnGallery = findViewById(R.id.btnGallery);
        btnSave = findViewById(R.id.btnSave);
        btnColor = findViewById(R.id.btnColor);
        textControls = findViewById(R.id.textControls);
        imageControls = findViewById(R.id.imageControls);
        seekTextSize = findViewById(R.id.seekTextSize);
        seekImageSize = findViewById(R.id.seekImageSize);
        seekTextRotate = findViewById(R.id.seekTextRotate);
        seekImageRotate = findViewById(R.id.seekImageRotate);
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
        textLayerControlsLayout = findViewById(R.id.textLayerControlsLayout);
        imageLayerControlsLayout = findViewById(R.id.imageLayerControlsLayout);

        btnFont = findViewById(R.id.btnFont);
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);

        uid = FirebaseAuth.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // ==== Load image ====
        originalPath = getIntent().getStringExtra("uri");

        if (originalPath != null) {
            templateId = makeSafeKey(originalPath);

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
                // 🖼️ Default Ratio for Image (1:1)
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) canvasContainer
                        .getLayoutParams();
                lp.dimensionRatio = "1:1";
                canvasContainer.setLayoutParams(lp);

                loadImageSmart(originalPath, imgTemplate);
            }
        }

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

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
        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && activeContent instanceof TextView) {
                    float newSize = 10 + (progress * 2);
                    ((TextView) activeContent).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newSize);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekTextRotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && activeOverlay != null && activeContent instanceof TextView) {
                    float rotation = progress - 180; // center is 0 degrees
                    activeOverlay.setRotation(rotation);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekImageSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && activeOverlay != null && activeContent instanceof ImageView) {
                    float scale = 0.1f + (progress / 40f);
                    activeOverlay.setScaleX(scale);
                    activeOverlay.setScaleY(scale);

                    if (((ViewGroup) activeOverlay).getChildCount() > 1) {
                        View btnDel = ((ViewGroup) activeOverlay).getChildAt(1);
                        btnDel.setScaleX(1.0f / scale);
                        btnDel.setScaleY(1.0f / scale);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekImageRotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && activeOverlay != null && activeContent instanceof ImageView) {
                    float rotation = progress - 180; // center is 0 degrees
                    activeOverlay.setRotation(rotation);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

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
        btnSave.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.title_save_template)
                .setMessage(R.string.msg_save_confirm)
                .setPositiveButton(R.string.btn_yes,
                        (d, w) -> saveFinalImage())
                .setNegativeButton(R.string.btn_no, null)
                .show());

    }

    // ================= TEXT EDIT =================
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
        tv.setPadding(30, 30, 30, 30);
        addOverlay(tv, false);
    }

    private void addNewImage(Uri uri) {
        com.google.android.material.imageview.ShapeableImageView iv = new com.google.android.material.imageview.ShapeableImageView(
                this);
        iv.setImageURI(uri);
        iv.setAdjustViewBounds(true);
        iv.setPadding(30, 30, 30, 30);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

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

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setLayoutParams(
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Content
        wrapper.addView(content);

        // Delete Button
        ImageView btnDel = new ImageView(this);
        btnDel.setImageResource(R.drawable.ic_delete);
        btnDel.setBackgroundResource(R.drawable.shape_circle);
        btnDel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
        btnDel.setPadding(10, 10, 10, 10);
        FrameLayout.LayoutParams delParams = new FrameLayout.LayoutParams(60, 60);
        delParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        btnDel.setLayoutParams(delParams);
        btnDel.setOnClickListener(v -> {
            if (activeOverlay == wrapper) {
                deselectAll();
            }
            ((ViewGroup) wrapper.getParent()).removeView(wrapper);
        });
        wrapper.addView(btnDel);

        wrapper.setX(200);
        wrapper.setY(200);
        canvas.addView(wrapper);

        enableDrag(wrapper, content);
        setActive(wrapper);

        content.setOnClickListener(v -> {
            if (activeOverlay == wrapper) {
                if (!isImage) {
                    showTextEditor((TextView) content);
                } else {
                    Object tag = content.getTag();
                    int currentShape = tag instanceof Integer ? (Integer) tag : 0;
                    int nextShape = (currentShape + 1) % 2; // Only Rect(0) and Circle(1)
                    content.setTag(nextShape);
                    applyShape((com.google.android.material.imageview.ShapeableImageView) content, nextShape);
                }
            } else {
                setActive(wrapper);
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
            }
        });
        dialog.show();
    }

    private void setActive(View wrapper) {
        deselectAll();
        activeOverlay = wrapper;

        if (wrapper == null)
            return;

        activeContent = ((FrameLayout) wrapper).getChildAt(0);

        // Show handles
        if (((FrameLayout) wrapper).getChildCount() > 1) {
            ((FrameLayout) wrapper).getChildAt(1).setVisibility(View.VISIBLE); // Delete
        }

        if (activeContent instanceof TextView) {
            textControls.setVisibility(View.VISIBLE);
            imageControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);

            TextView tv = (TextView) activeContent;
            float currentSize = tv.getTextSize();
            int progress = (int) ((currentSize - 10) / 2);
            seekTextSize.setProgress(Math.max(0, Math.min(100, progress)));

            float rotation = wrapper.getRotation() + 180;
            seekTextRotate.setProgress(Math.max(0, Math.min(360, (int) rotation)));

            // Sync text styles
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

            float currentScale = wrapper.getScaleX();
            int progress = (int) ((currentScale - 0.1f) * 40f);
            seekImageSize.setProgress(Math.max(0, Math.min(100, progress)));

            float rotation = wrapper.getRotation() + 180;
            seekImageRotate.setProgress(Math.max(0, Math.min(360, (int) rotation)));
        }
    }

    private void deselectAll() {
        if (canvas == null)
            return;
        for (int i = 0; i < canvas.getChildCount(); i++) {
            View child = canvas.getChildAt(i);
            if (child instanceof FrameLayout && child != dynamicFrameContainer) {
                if (((FrameLayout) child).getChildCount() > 1) {
                    ((FrameLayout) child).getChildAt(1).setVisibility(View.GONE);
                }
            }
        }
        activeOverlay = null;
        activeContent = null;
        if (textControls != null)
            textControls.setVisibility(View.GONE);
        if (imageControls != null)
            imageControls.setVisibility(View.GONE);
        if (frameControls != null)
            frameControls.setVisibility(View.GONE);
    }

    // ================= DRAG & RESIZE =================
    void enableDrag(View wrapper, View content) {
        content.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float startX, startY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (activeOverlay != wrapper)
                            setActive(wrapper);
                        dX = wrapper.getX() - e.getRawX();
                        dY = wrapper.getY() - e.getRawY();
                        startX = e.getRawX();
                        startY = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        wrapper.setX(e.getRawX() + dX);
                        wrapper.setY(e.getRawY() + dY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(e.getRawX() - startX);
                        float diffY = Math.abs(e.getRawY() - startY);
                        if (diffX < 10 && diffY < 10) {
                            if (activeOverlay == wrapper) {
                                v.performClick();
                            } else {
                                setActive(wrapper);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // ================= SAVE IMAGE =================
    void saveFinalImage() {
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

            String safeId = makeSafeKey(originalPath);

            rootRef.child("template_activity")
                    .child(safeId)
                    .child("edits")
                    .child(uid)
                    .setValue(true);

            rootRef.child("user_activity")
                    .child(uid)
                    .child("edits")
                    .child(safeId)
                    .setValue(savedPath);
        } else {
            Toast.makeText(this, R.string.msg_failed_save_image, Toast.LENGTH_SHORT).show();
        }
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
        progressText.setText("0%");
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

        com.arthenica.mobileffmpeg.Config.enableStatisticsCallback(newStatistics -> {
            int time = newStatistics.getTime();
            int progress = (int) ((time * 100) / finalDuration);
            if (progress > 100)
                progress = 100;
            if (progress < 0)
                progress = 0;

            final int p = progress;
            runOnUiThread(() -> {
                pb.setProgress(p);
                progressText.setText(p + "%");
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
            com.arthenica.mobileffmpeg.FFmpeg.executeAsync(command, new com.arthenica.mobileffmpeg.ExecuteCallback() {
                @Override
                public void apply(long executionId, int returnCode) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        com.arthenica.mobileffmpeg.Config.enableStatisticsCallback(null); // clear callback
                        if (returnCode == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                            Toast.makeText(ManageTemplatesActivity.this, R.string.msg_video_saved_success,
                                    Toast.LENGTH_LONG).show();

                            android.content.Intent mediaScanIntent = new android.content.Intent(
                                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            android.net.Uri contentUri = android.net.Uri.fromFile(outputFile);
                            mediaScanIntent.setData(contentUri);
                            sendBroadcast(mediaScanIntent);

                            String safeId = makeSafeKey(originalPath);
                            rootRef.child("user_activity").child(uid).child("edits").child(safeId)
                                    .setValue(outputFile.getAbsolutePath());
                        } else {
                            Toast.makeText(ManageTemplatesActivity.this,
                                    getString(R.string.msg_failed_save_video_error, returnCode), Toast.LENGTH_LONG).show();
                        }
                    });
                }
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
                "Default Sans", "Serif", "Monospace",
                "Lobster (Cursive)", "Pacifico (Handwriting)", "Anton (Heavy)",
                "Righteous (Display)", "Bebas Neue", "Medium", "Black"
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

    private String makeSafeKey(String value) {
        if (value == null)
            return "Unknown";
        return android.util.Base64.encodeToString(value.getBytes(), android.util.Base64.NO_WRAP).replace(".", "_")
                .replace("$", "_").replace("#", "_");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(ev);
        }

        int action = ev.getActionMasked();

        // Multi-touch Pan (e.g. 2 fingers)
        if (ev.getPointerCount() > 1) {
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

}