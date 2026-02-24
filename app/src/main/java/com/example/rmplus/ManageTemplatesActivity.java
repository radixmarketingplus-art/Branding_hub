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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ManageTemplatesActivity extends AppCompatActivity {

    ImageView imgTemplate, imgFrame, imgLogo;
    TextView txtEdit;
    ImageButton btnText, btnFrame, btnSave;
    Button btnLogo, btnColor;
    ViewPager2 vpFrames;
    ArrayList<String> slidingFrames = new ArrayList<>();
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
        vpFrames = findViewById(R.id.vpFrames);


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
            vpFrames.setVisibility(View.VISIBLE);
            rvFrames.setVisibility(View.GONE);
            seekFrameSize.setVisibility(View.GONE);
            textControls.setVisibility(View.GONE);
            logoControls.setVisibility(View.GONE);
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

                    loadImageSmart(path, imgFrame);

                    // 🔥 FIX FRAME SIZE = SQUARE OVERLAY (1080x1080 aspect)
                    // We match parent but ensure parent canvas is square in layout
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                            );
                    imgFrame.setLayoutParams(params);

                    imgFrame.setScaleType(ImageView.ScaleType.FIT_XY);
                    imgFrame.setVisibility(View.VISIBLE);
                    
                    // Frames are now fixed overlays, no dragging
                    imgFrame.setOnTouchListener(null); 

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

    private void loadImageSmart(String path, ImageView imageView) {
        if (path == null || path.isEmpty()) return;

        // 🌐 LOAD FROM VPS URL OR LOCAL FILE (Glide handles both)
        Glide.with(this)
                .load(path)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
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

    private void loadSlidingFrames() {
        rootRef.child("templates").child("Frame")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        slidingFrames.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String f = d.hasChild("url") ? d.child("url").getValue(String.class) : d.child("imagePath").getValue(String.class);
                            if (f != null) slidingFrames.add(f);
                        }

                        if (!slidingFrames.isEmpty()) {
                            vpFrames.setAdapter(new FrameOverlayAdapter(slidingFrames));
                            
                            // Check for passed frame from intent
                            String passedFrame = getIntent().getStringExtra("selected_frame");
                            int startIndex = (Integer.MAX_VALUE / 2);
                            startIndex = startIndex - (startIndex % (slidingFrames.size() + 1));
                            
                            if (passedFrame != null) {
                                for(int i=0; i<slidingFrames.size(); i++) {
                                    if(slidingFrames.get(i).equals(passedFrame)) {
                                        startIndex += (i + 1);
                                        vpFrames.setVisibility(View.VISIBLE);
                                        break;
                                    }
                                }
                            }
                            
                            vpFrames.setCurrentItem(startIndex, false);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    class FrameOverlayAdapter extends RecyclerView.Adapter<FrameOverlayAdapter.VH> {
        ArrayList<String> frames;
        FrameOverlayAdapter(ArrayList<String> frames) { this.frames = frames; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

        @Override public int getItemCount() { return frames.size() > 0 ? Integer.MAX_VALUE : 0; }

        class VH extends RecyclerView.ViewHolder {
            ImageView img;
            VH(View v) { super(v); img = (ImageView) v; }
        }
    }

}