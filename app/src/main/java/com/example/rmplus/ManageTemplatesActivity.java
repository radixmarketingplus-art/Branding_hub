package com.example.rmplus;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ManageTemplatesActivity extends AppCompatActivity {

    ImageView imgTemplate, imgFrame, imgLogo;
    TextView txtEdit;
    ImageButton btnText, btnFrame, btnSave;
    Button btnLogo, btnColor;
    LinearLayout textControls, logoControls;
    SeekBar seekTextSize, seekLogoSize;
    ImageView imgDelete;

    static final int PICK_LOGO = 101;
    float dX, dY;

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
        imgTemplate = findViewById(R.id.imgTemplate);
        imgFrame = findViewById(R.id.imgFrame);
        imgLogo = findViewById(R.id.imgLogo);
        txtEdit = findViewById(R.id.txtEdit);
        imgDelete = findViewById(R.id.imgDelete);
        btnText = findViewById(R.id.btnText);
        btnLogo = findViewById(R.id.btnLogo);
        btnFrame = findViewById(R.id.btnFrame);
        btnSave = findViewById(R.id.btnSave);
        btnColor = findViewById(R.id.btnColor);
        textControls = findViewById(R.id.textControls);
        logoControls = findViewById(R.id.logoControls);
        seekTextSize = findViewById(R.id.seekTextSize);
        seekLogoSize = findViewById(R.id.seekLogoSize);
        frameControls = findViewById(R.id.frameControls);
        rvFrames = findViewById(R.id.rvFrames);
        seekFrameSize = findViewById(R.id.seekFrameSize);


        uid = FirebaseAuth.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // ==== Load image ====
        originalPath = getIntent().getStringExtra("uri");

        if (originalPath != null) {
            templateId = makeSafeKey(originalPath);
            imgTemplate.setImageURI(
                    Uri.fromFile(new java.io.File(originalPath))
            );
        }

        imgLogo.setClipToOutline(true);
        imgLogo.setBackgroundResource(R.drawable.shape_circle);

        // ==== Drag ====
        enableDrag(txtEdit);
        enableDrag(imgLogo);
        enableDrag(imgFrame);

        // ==== TEXT ====
        btnText.setOnClickListener(v -> {
            txtEdit.setText("Edit Text");
            txtEdit.setX(100);
            txtEdit.setY(100);
            txtEdit.setVisibility(View.VISIBLE);

            textControls.setVisibility(View.VISIBLE);
            logoControls.setVisibility(View.GONE);

            showTextEditor();
        });

        // ==== LOGO ====
        btnLogo.setOnClickListener(v -> {
            imgLogo.setX(100);
            imgLogo.setY(100);
            imgLogo.setVisibility(View.VISIBLE);

            logoControls.setVisibility(View.VISIBLE);
            textControls.setVisibility(View.GONE);

            pickLogoFromGallery();
        });

        // ==== FRAME ====
        btnFrame.setOnClickListener(v -> {

            frameControls.setVisibility(View.VISIBLE);

            rvFrames.setVisibility(View.VISIBLE);
            seekFrameSize.setVisibility(View.GONE);

            textControls.setVisibility(View.GONE);
            logoControls.setVisibility(View.GONE);

        });

        // ==== COLOR ====
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
                    }
            ).show();

        });

        // ==== TEXT SIZE ====
        seekTextSize.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar s, int p, boolean f) {
                        if (p < 10) p = 10;
                        txtEdit.setTextSize(p);
                    }
                    public void onStartTrackingTouch(SeekBar s) {}
                    public void onStopTrackingTouch(SeekBar s) {}
                });

        // ==== LOGO SIZE ====
        seekLogoSize.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar s, int p, boolean f) {
                        if (p < 50) p = 50;
                        imgLogo.getLayoutParams().width = p;
                        imgLogo.getLayoutParams().height = p;
                        imgLogo.requestLayout();
                    }
                    public void onStartTrackingTouch(SeekBar s) {}
                    public void onStopTrackingTouch(SeekBar s) {}
                });



        // ==== SAVE ====
        btnSave.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Save Edited Template")
                        .setMessage("Do you really want to save?")
                        .setPositiveButton("Yes",
                                (d, w) -> saveFinalImage())
                        .setNegativeButton("No", null)
                        .show()
        );

        String frameKey = getIntent().getStringExtra("frames_key");

// 🔥 FALLBACK LOGIC (VERY IMPORTANT)
        if (frameKey == null) {
            frameKey = "Business Frame";
        }

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

// 🔥 SAME CATEGORY LIST USED AS FRAMES
        String json = sp.getString(frameKey, null);

        if (json == null) {
            Toast.makeText(this,
                    "No frames found for " + frameKey,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        java.lang.reflect.Type type =
                new com.google.gson.reflect.TypeToken<
                        java.util.ArrayList<String>>() {}.getType();

        java.util.ArrayList<String> frames =
                new com.google.gson.Gson().fromJson(json, type);

        if (frames == null || frames.isEmpty()) {
            Toast.makeText(this,
                    "Frame list empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        rvFrames.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(
                        this,
                        androidx.recyclerview.widget.RecyclerView.HORIZONTAL,
                        false
                )
        );

        rvFrames.setAdapter(
                new FrameAdapter(frames, path -> {

                    imgFrame.setImageURI(
                            Uri.fromFile(new java.io.File(path))
                    );

                    // 🔥 FIX FRAME SIZE = MATCH TEMPLATE
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                            );
                    imgFrame.setLayoutParams(params);

                    imgFrame.setScaleType(ImageView.ScaleType.FIT_XY);
                    imgFrame.setVisibility(View.VISIBLE);

                    // ❌ NO SIZE CONTROL
                    rvFrames.setVisibility(View.GONE);
                    seekFrameSize.setVisibility(View.GONE);
                })
        );
    }

    // ================= TEXT EDIT =================
    void showTextEditor() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        EditText input = new EditText(this);
        input.setText(txtEdit.getText());
        b.setTitle("Edit Text");
        b.setView(input);
        b.setPositiveButton("Apply",
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
            imgLogo.setImageURI(data.getData());
        }
    }

    // ================= DRAG =================
    void enableDrag(View v) {

        GestureDetector detector = null;

        if (v == txtEdit) {
            detector = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        public boolean onDoubleTap(MotionEvent e) {
                            showTextEditor();
                            return true;
                        }
                    });
        }

        GestureDetector finalDetector = detector;

        v.setOnTouchListener((view, e) -> {

            if (finalDetector != null)
                finalDetector.onTouchEvent(e);

            switch (e.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - e.getRawX();
                    dY = view.getY() - e.getRawY();
                    imgDelete.setVisibility(View.VISIBLE);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    view.setX(e.getRawX() + dX);
                    view.setY(e.getRawY() + dY);
                    return true;

                case MotionEvent.ACTION_UP:
                    imgDelete.setVisibility(View.GONE);

                    if (isOverDelete(view)) {

                        if (view == txtEdit) {
                            txtEdit.setVisibility(View.GONE);

                            // 🔥 HIDE TEXT CONTROLS
                            textControls.setVisibility(View.GONE);
                        }

                        if (view == imgLogo) {
                            imgLogo.setVisibility(View.GONE);

                            // 🔥 HIDE LOGO CONTROLS
                            logoControls.setVisibility(View.GONE);
                        }

                        if (view == imgFrame) {
                            imgFrame.setVisibility(View.GONE);
                            frameControls.setVisibility(View.GONE);
                            rvFrames.setVisibility(View.VISIBLE);
                            seekFrameSize.setVisibility(View.GONE);
                        }
                    }
                    return true;
            }
            return false;
        });
    }

    boolean isOverDelete(View v) {
        int[] a = new int[2];
        int[] b = new int[2];
        v.getLocationOnScreen(a);
        imgDelete.getLocationOnScreen(b);

        int cx = a[0] + v.getWidth()/2;
        int cy = a[1] + v.getHeight()/2;

        return cx>b[0] && cx<b[0]+imgDelete.getWidth()
                && cy>b[1] && cy<b[1]+imgDelete.getHeight();
    }

    // ================= SAVE IMAGE =================
    void saveFinalImage() {

        View canvas = findViewById(R.id.canvas);
        canvas.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(canvas.getDrawingCache());
        canvas.setDrawingCacheEnabled(false);

        String savedPath =
                MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        "RMPlus_Edit_" + System.currentTimeMillis(),
                        "Edited Template"
                );

        if (savedPath != null) {

            Toast.makeText(this,
                    "Saved to Gallery",
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
        if (txtEdit.getVisibility() != View.VISIBLE) {
            textControls.setVisibility(View.GONE);
        }

        if (imgLogo.getVisibility() != View.VISIBLE) {
            logoControls.setVisibility(View.GONE);
        }
    }
    private String makeSafeKey(String value){
        return android.util.Base64.encodeToString(
                value.getBytes(),
                android.util.Base64.NO_WRAP
        );
    }

}