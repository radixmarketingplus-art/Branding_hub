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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
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

    FrameLayout wrapText, wrapLogo, wrapGallery;
    ImageView imgTemplate, imgFrame, imgLogo, imgGallery;
    ImageView btnDeleteText, btnDeleteLogo, btnDeleteGallery;
    TextView txtEdit;
    com.google.android.material.button.MaterialButton btnSave;
    Button btnColor;
    LinearLayout btnText, btnLogo, btnFrame, btnGallery;
    ViewPager2 vpFrames;
    ArrayList<String> slidingFrames = new ArrayList<>();
    LinearLayout textControls, imageControls;

    static final int PICK_LOGO = 101;
    static final int PICK_GALLERY = 102;
    SeekBar seekTextSize, seekImageSize;
    View scaleLogo, scaleGallery; // New containers for scaling content only

    // 🔥 NEW
    String originalPath;
    String templateId;
    String uid;
    DatabaseReference rootRef;
    LinearLayout frameControls;
    RecyclerView rvFrames;
    SeekBar seekFrameSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_templates);

        // ==== Bind ====
        wrapText = findViewById(R.id.wrapText);
        wrapLogo = findViewById(R.id.wrapLogo);
        wrapGallery = findViewById(R.id.wrapGallery);

        btnDeleteText = findViewById(R.id.btnDeleteText);
        btnDeleteLogo = findViewById(R.id.btnDeleteLogo);
        btnDeleteGallery = findViewById(R.id.btnDeleteGallery);

        imgTemplate = findViewById(R.id.imgTemplate);
        imgFrame = findViewById(R.id.imgFrame);
        imgLogo = findViewById(R.id.imgLogo);
        imgGallery = findViewById(R.id.imgGallery);
        txtEdit = findViewById(R.id.txtEdit);
        btnText = findViewById(R.id.btnText);
        btnLogo = findViewById(R.id.btnLogo);
        btnGallery = findViewById(R.id.btnGallery);
        btnFrame = findViewById(R.id.btnFrame);
        btnSave = findViewById(R.id.btnSave);
        btnColor = findViewById(R.id.btnColor);
        textControls = findViewById(R.id.textControls);
        imageControls = findViewById(R.id.imageControls);
        frameControls = findViewById(R.id.frameControls);
        rvFrames = findViewById(R.id.rvFrames);
        seekFrameSize = findViewById(R.id.seekFrameSize);
        vpFrames = findViewById(R.id.vpFrames);
        seekTextSize = findViewById(R.id.seekTextSize);
        seekImageSize = findViewById(R.id.seekImageSize);
        scaleLogo = findViewById(R.id.scaleLogo);
        scaleGallery = findViewById(R.id.scaleGallery);

        uid = FirebaseAuth.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // ==== Load image ====
        originalPath = getIntent().getStringExtra("uri");

        if (originalPath != null) {
            templateId = makeSafeKey(originalPath);
            loadImageSmart(originalPath, imgTemplate);
        }

        imgLogo.setClipToOutline(true);
        imgLogo.setBackgroundResource(R.drawable.shape_circle);

        // ==== Drag & Selection Logic ====
        enableDrag(wrapText, null);
        enableDrag(wrapLogo, scaleLogo);
        enableDrag(wrapGallery, scaleGallery);

        // Add Tap-to-select behavior
        wrapText.setOnClickListener(v -> {
            textControls.setVisibility(View.VISIBLE);
            imageControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);
        });

        wrapLogo.setOnClickListener(v -> {
            imageControls.setVisibility(View.VISIBLE);
            textControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);
            // Sync SeekBar to current scale
            float currentScale = scaleLogo.getScaleX();
            int progress = (int) ((currentScale - 0.2f) * 50f);
            seekImageSize.setProgress(Math.max(0, Math.min(100, progress)));
        });

        wrapGallery.setOnClickListener(v -> {
            imageControls.setVisibility(View.VISIBLE);
            textControls.setVisibility(View.GONE);
            frameControls.setVisibility(View.GONE);
            // Sync SeekBar to current scale
            float currentScale = scaleGallery.getScaleX();
            int progress = (int) ((currentScale - 0.2f) * 50f);
            seekImageSize.setProgress(Math.max(0, Math.min(100, progress)));
        });

        btnDeleteText.setOnClickListener(v -> {
            wrapText.setVisibility(View.GONE);
            txtEdit.setText(R.string.ph_edit_text);
            textControls.setVisibility(View.GONE);
        });
        btnDeleteLogo.setOnClickListener(v -> {
            wrapLogo.setVisibility(View.GONE);
            imgLogo.setImageDrawable(null);
            imageControls.setVisibility(View.GONE);
        });
        btnDeleteGallery.setOnClickListener(v -> {
            wrapGallery.setVisibility(View.GONE);
            imgGallery.setImageDrawable(null);
        });

        // ==== TEXT ====
        btnText.setOnClickListener(v -> {
            txtEdit.setText(R.string.ph_edit_text);
            wrapText.setX(100);
            wrapText.setY(100);
            wrapText.setVisibility(View.VISIBLE);

            textControls.setVisibility(View.VISIBLE);
            imageControls.setVisibility(View.GONE);

            showTextEditor();
        });

        // ==== LOGO ====
        btnLogo.setOnClickListener(v -> {
            textControls.setVisibility(View.GONE);
            pickLogoFromGallery();
        });

        // ==== GALLERY ====
        btnGallery.setOnClickListener(v -> {
            textControls.setVisibility(View.GONE);
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, PICK_GALLERY);
        });

        // ==== SEEKBARS ====
        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newSize = 20 + (progress * 3); // 20px to ~320px
                    txtEdit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newSize);
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
                if (fromUser) {
                    float scale = 0.2f + (progress / 50f); // 0.2x to ~2.2x
                    if (wrapLogo.getVisibility() == View.VISIBLE && imageControls.getVisibility() == View.VISIBLE) {
                        scaleLogo.setScaleX(scale);
                        scaleLogo.setScaleY(scale);
                        // Fix delete icon position & size by inverting the scale
                        btnDeleteLogo.setScaleX(1.0f / scale);
                        btnDeleteLogo.setScaleY(1.0f / scale);
                    } else if (wrapGallery.getVisibility() == View.VISIBLE
                            && imageControls.getVisibility() == View.VISIBLE) {
                        scaleGallery.setScaleX(scale);
                        scaleGallery.setScaleY(scale);
                        btnDeleteGallery.setScaleX(1.0f / scale);
                        btnDeleteGallery.setScaleY(1.0f / scale);
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

        // ==== FRAME ====
        btnFrame.setOnClickListener(v -> {
            frameControls.setVisibility(View.VISIBLE);
            vpFrames.setVisibility(View.VISIBLE);
            rvFrames.setVisibility(View.GONE);
            seekFrameSize.setVisibility(View.GONE);
            textControls.setVisibility(View.GONE);
            imageControls.setVisibility(View.GONE);
        });

        // ==== COLOR ====
        loadSlidingFrames();
        btnColor.setOnClickListener(v -> {
            new AmbilWarnaDialog(
                    this,
                    txtEdit.getCurrentTextColor(),
                    new AmbilWarnaDialog.OnAmbilWarnaListener() {

                        @Override
                        public void onOk(AmbilWarnaDialog dialog, int color) {
                            txtEdit.setTextColor(color);
                        }

                        @Override
                        public void onCancel(AmbilWarnaDialog dialog) {
                            // nothing
                        }
                    }).show();

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

        String frameKey = getIntent().getStringExtra("frames_key");

        // 🔥 FALLBACK LOGIC (VERY IMPORTANT)
        if (frameKey == null) {
            frameKey = "Business Frame";
        }

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        // 🔥 SAME CATEGORY LIST USED AS FRAMES
        String json = sp.getString(frameKey, null);

        if (json == null) {
            Toast.makeText(this,
                    getString(R.string.msg_no_frames_found, frameKey),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.ArrayList<String>>() {
        }.getType();

        java.util.ArrayList<String> frames = new com.google.gson.Gson().fromJson(json, type);

        if (frames == null || frames.isEmpty()) {
            Toast.makeText(this,
                    R.string.msg_frame_list_empty,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        rvFrames.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(
                        this,
                        androidx.recyclerview.widget.RecyclerView.HORIZONTAL,
                        false));

        rvFrames.setAdapter(
                new FrameAdapter(frames, path -> {

                    loadImageSmart(path, imgFrame);

                    // 🔥 FIX FRAME SIZE = SQUARE OVERLAY (1080x1080 aspect)
                    // We match parent but ensure parent canvas is square in layout
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
                    imgFrame.setLayoutParams(params);

                    imgFrame.setScaleType(ImageView.ScaleType.FIT_XY);
                    imgFrame.setVisibility(View.VISIBLE);

                    // Frames are now fixed overlays, no dragging
                    imgFrame.setOnTouchListener(null);

                    rvFrames.setVisibility(View.GONE);
                    seekFrameSize.setVisibility(View.GONE);
                }));
    }

    // ================= TEXT EDIT =================
    void showTextEditor() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        EditText input = new EditText(this);
        input.setText(txtEdit.getText());
        b.setTitle(R.string.ph_edit_text);
        b.setView(input);
        b.setPositiveButton(R.string.btn_apply,
                (d, w) -> txtEdit.setText(input.getText()));
        b.show();
    }

    // ================= PICK LOGO =================
    void pickLogoFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_LOGO);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == PICK_LOGO && res == RESULT_OK && data != null) {
            startCrop(data.getData(), UCrop.REQUEST_CROP);
        }
        if (rc == PICK_GALLERY && res == RESULT_OK && data != null) {
            startCrop(data.getData(), UCrop.REQUEST_CROP + 1);
        }

        if (rc == UCrop.REQUEST_CROP && res == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                imgLogo.setImageURI(null);
                imgLogo.setImageURI(resultUri);
                wrapLogo.setVisibility(View.VISIBLE);
                wrapLogo.setX(100);
                wrapLogo.setY(100);

                scaleLogo.setScaleX(0.8f);
                scaleLogo.setScaleY(0.8f);
                btnDeleteLogo.setScaleX(1.0f / 0.8f);
                btnDeleteLogo.setScaleY(1.0f / 0.8f);
                seekImageSize.setProgress(30);

                imageControls.setVisibility(View.VISIBLE);
                textControls.setVisibility(View.GONE);
                wrapLogo.requestLayout();
            }
        }

        if (rc == UCrop.REQUEST_CROP + 1 && res == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                imgGallery.setImageURI(null);
                imgGallery.setImageURI(resultUri);
                wrapGallery.setVisibility(View.VISIBLE);
                wrapGallery.setX(150);
                wrapGallery.setY(150);

                scaleGallery.setScaleX(0.8f);
                scaleGallery.setScaleY(0.8f);
                btnDeleteGallery.setScaleX(1.0f / 0.8f);
                btnDeleteGallery.setScaleY(1.0f / 0.8f);
                seekImageSize.setProgress(30);

                imageControls.setVisibility(View.VISIBLE);
                textControls.setVisibility(View.GONE);
                wrapGallery.requestLayout();
            }
        }
    }

    private void startCrop(Uri uri, int requestCode) {
        String destinationFileName = "RMPlus_Crop_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withOptions(new UCrop.Options()); // Default free-style crop
        uCrop.start(this, requestCode);
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

    // ================= DRAG & SCALE =================
    void enableDrag(View wrapperView, View innerScaledView) {

        GestureDetector detector = null;

        if (wrapperView == wrapText) {
            detector = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        public boolean onDoubleTap(MotionEvent e) {
                            showTextEditor();
                            return true;
                        }
                    });
        }

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float scaleFactor = detector.getScaleFactor();
                        if (wrapperView == wrapText) {
                            float currentSize = txtEdit.getTextSize();
                            float newSize = currentSize * scaleFactor;
                            if (newSize < 20)
                                newSize = 20;
                            if (newSize > 500)
                                newSize = 500;
                            txtEdit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newSize);
                        } else if (innerScaledView != null) {
                            innerScaledView.setPivotX(detector.getFocusX());
                            innerScaledView.setPivotY(detector.getFocusY());
                            float nX = innerScaledView.getScaleX() * scaleFactor;
                            float nY = innerScaledView.getScaleY() * scaleFactor;
                            if (nX < 0.1f)
                                nX = 0.1f;
                            if (nY < 0.1f)
                                nY = 0.1f;
                            innerScaledView.setScaleX(nX);
                            innerScaledView.setScaleY(nY);

                            // Apply inverse scale to delete buttons to keep them fixed size and attached
                            if (innerScaledView == scaleLogo) {
                                btnDeleteLogo.setScaleX(1.0f / nX);
                                btnDeleteLogo.setScaleY(1.0f / nY);
                            } else if (innerScaledView == scaleGallery) {
                                btnDeleteGallery.setScaleX(1.0f / nX);
                                btnDeleteGallery.setScaleY(1.0f / nY);
                            }
                        }
                        return true;
                    }
                });

        GestureDetector finalDetector = detector;

        wrapperView.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent e) {

                boolean handled = scaleDetector.onTouchEvent(e);

                if (finalDetector != null)
                    finalDetector.onTouchEvent(e);

                if (scaleDetector.isInProgress()) {
                    return true;
                }

                switch (e.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - e.getRawX();
                        dY = view.getY() - e.getRawY();
                        handled = true;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (e.getPointerCount() == 1) {
                            view.setX(e.getRawX() + dX);
                            view.setY(e.getRawY() + dY);
                        }
                        handled = true;
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handled = true;
                        break;
                }
                return handled;
            }
        });
    }

    // ================= SAVE IMAGE =================
    void saveFinalImage() {
        // Hide delete buttons before saving
        btnDeleteText.setVisibility(View.GONE);
        if (btnDeleteLogo != null)
            btnDeleteLogo.setVisibility(View.GONE);
        if (btnDeleteGallery != null)
            btnDeleteGallery.setVisibility(View.GONE);

        View canvas = findViewById(R.id.canvas);
        canvas.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(canvas.getDrawingCache());
        canvas.setDrawingCacheEnabled(false);

        // Restore delete buttons
        if (wrapText.getVisibility() == View.VISIBLE)
            btnDeleteText.setVisibility(View.VISIBLE);
        if (wrapLogo.getVisibility() == View.VISIBLE)
            btnDeleteLogo.setVisibility(View.VISIBLE);
        if (wrapGallery.getVisibility() == View.VISIBLE)
            btnDeleteGallery.setVisibility(View.VISIBLE);

        String savedPath = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "RMPlus_Edit_" + System.currentTimeMillis(),
                getString(R.string.desc_edited_template));

        if (savedPath != null) {

            Toast.makeText(this,
                    R.string.msg_saved_to_gallery,
                    Toast.LENGTH_SHORT).show();

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
        }
    }

    void resetControlsIfNothingSelected() {
        if (wrapText.getVisibility() != View.VISIBLE) {
            textControls.setVisibility(View.GONE);
        }

        if (wrapLogo.getVisibility() != View.VISIBLE && wrapGallery.getVisibility() != View.VISIBLE) {
            imageControls.setVisibility(View.GONE);
        }
    }

    private String makeSafeKey(String value) {
        return android.util.Base64.encodeToString(
                value.getBytes(),
                android.util.Base64.NO_WRAP);
    }

    // Assuming these lines are meant to be in an initialization method like
    // onCreate
    // or a similar setup method, not inside makeSafeKey.
    // The instruction implies they are part of a method that also sets up the back
    // button.
    // For now, placing them after makeSafeKey and before loadSlidingFrames,
    // assuming they belong to a new or existing setup method.
    // If this is part of onCreate, the user should provide the full onCreate
    // method.
    // Based on the instruction's context, it seems to be a new block of code.
    // I'll place it as a new method or block, respecting the indentation.
    // Since the instruction shows `findViewById(R.id.btnBack).setOnClickListener(v
    // -> finish());`
    // followed by `private void prepareCanvas() { ... }`, and `prepareCanvas` is
    // not in the original,
    // I will assume `loadFrames()` and `loadActiveSubscription()` are also part of
    // this new block.
    // I'll create a placeholder method for these.

    // Placeholder for new initialization logic
    private void setupActivity() {
        // loadFrames(); // This method is not defined in the provided code
        // loadActiveSubscription(); // This method is not defined in the provided code

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadSlidingFrames() {
        rootRef.child("templates").child("Frame")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        slidingFrames.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String f = d.hasChild("url") ? d.child("url").getValue(String.class)
                                    : d.child("imagePath").getValue(String.class);
                            if (f != null)
                                slidingFrames.add(f);
                        }

                        if (!slidingFrames.isEmpty()) {
                            vpFrames.setAdapter(new FrameOverlayAdapter(slidingFrames));

                            // Check for passed frame from intent
                            String passedFrame = getIntent().getStringExtra("selected_frame");
                            int startIndex = (Integer.MAX_VALUE / 2);
                            startIndex = startIndex - (startIndex % (slidingFrames.size() + 1));

                            if (passedFrame != null) {
                                for (int i = 0; i < slidingFrames.size(); i++) {
                                    if (slidingFrames.get(i).equals(passedFrame)) {
                                        startIndex += (i + 1);
                                        vpFrames.setVisibility(View.VISIBLE);
                                        break;
                                    }
                                }
                            }

                            vpFrames.setCurrentItem(startIndex, false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    class FrameOverlayAdapter extends RecyclerView.Adapter<FrameOverlayAdapter.VH> {
        ArrayList<String> frames;

        FrameOverlayAdapter(ArrayList<String> frames) {
            this.frames = frames;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            return new VH(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            int actualPos = pos % (frames.size() + 1);
            if (actualPos == 0) {
                h.img.setImageDrawable(null);
            } else {
                Glide.with(h.img.getContext())
                        .load(frames.get(actualPos - 1))
                        .into(h.img);
            }
        }

        @Override
        public int getItemCount() {
            return frames.size() > 0 ? Integer.MAX_VALUE : 0;
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView img;

            VH(View v) {
                super(v);
                img = (ImageView) v;
            }
        }
    }

}