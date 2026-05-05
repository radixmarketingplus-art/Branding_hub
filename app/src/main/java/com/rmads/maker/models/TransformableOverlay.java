package com.rmads.maker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Production-ready Canva-style transformable overlay.
 * Handles drag, corner-resize, and one-finger/two-finger rotation for
 * both TextView and ImageView content.
 *
 * Architecture:
 * - The wrapper (this) is an absolute-positioned FrameLayout on the canvas.
 * - Corner handles are drawn as child ImageViews translated OUTSIDE the wrapper bounds.
 * - setClipChildren(false) on this AND its parent canvas is required.
 * - Touch dispatch: handles get their own OnTouchListener; body drag is handled here.
 */
public class TransformableOverlay extends FrameLayout {

    // ─── Handle size & offset ───────────────────────────────────────────────
    private static final int HANDLE_SIZE_DP  = 40;   // hit area diameter
    private static final int VISIBLE_HANDLE_DP = 2;   // ultra-minimalistic
    private static final int PADDING_DP      = 40;   // extra space for handles
    private static final int ROTATE_OFFSET_DP = 10;  // tighter fit

    // ─── State ───────────────────────────────────────────────────────────────
    private View     content;
    private boolean  isSelected = false;

    // 8 handles: 0-3 corners, 4-7 sides
    private final ImageView[] handles = new ImageView[8];
    private ImageView rotateHandle;

    // drag state (body touch)
    private float dragDx, dragDy;

    // multitouch state
    private float lastPinchSpacing;
    private float lastPinchAngle;
    private float startScale;
    private float startRotation;
    private float startTextSize;
    private boolean isPinching;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector      gestureDetector;

    // click listener
    public interface OnOverlayClickListener {
        void onOverlaySelected(TransformableOverlay overlay);
    }
    private OnOverlayClickListener selectionListener;

    // ─── Constructors ────────────────────────────────────────────────────────
    public TransformableOverlay(@NonNull Context context) {
        super(context);
        init();
    }

    public TransformableOverlay(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(false); 
        int p = dp(PADDING_DP);
        setPadding(p, p, p, p);

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
        gestureDetector      = new GestureDetector(getContext(), new DoubleTapListener());
    }

    // ─── Public API ──────────────────────────────────────────────────────────
    public void setContent(View content) {
        this.content = content;
        removeAllViews();

        // Content is child 0
        addView(content, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // Ensure content touches reach this parent for dragging
        content.setOnTouchListener((v, event) -> onTouchEvent(event));

        buildHandles();
        setSelected(true);
    }

    public void setOnOverlayClickListener(OnOverlayClickListener l) {
        this.selectionListener = l;
    }

    public View getContent() { return content; }

    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        int vis = selected ? VISIBLE : GONE;
        for (ImageView h : handles) if (h != null) h.setVisibility(vis);
        if (rotateHandle != null) rotateHandle.setVisibility(vis);
        invalidate();
    }

    // ─── Build handles ───────────────────────────────────────────────────────
    private void buildHandles() {
        int hPx = dp(HANDLE_SIZE_DP);
        int p   = dp(PADDING_DP);

        // 0-3: Corners, 4-7: Sides
        int[] grav = {
            Gravity.TOP | Gravity.START, Gravity.TOP | Gravity.END,
            Gravity.BOTTOM | Gravity.START, Gravity.BOTTOM | Gravity.END,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
            Gravity.CENTER_VERTICAL | Gravity.START, Gravity.CENTER_VERTICAL | Gravity.END
        };

        for (int i = 0; i < 8; i++) {
            ImageView h = new ImageView(getContext());
            h.setBackgroundResource(R.drawable.shape_circle);
            h.setElevation(20f);
            
            int pad = dp((HANDLE_SIZE_DP - VISIBLE_HANDLE_DP) / 2);
            h.setPadding(pad, pad, pad, pad);
            h.setVisibility(GONE);

            LayoutParams lp = new LayoutParams(hPx, hPx, grav[i]);
            int m = p - (hPx / 2);
            
            // Adjust margins for sides to stay exactly on the line
            if (i == 4 || i == 5) lp.setMargins(0, m, 0, m); // Top/Bottom Center
            else if (i == 6 || i == 7) lp.setMargins(m, 0, m, 0); // Left/Right Center
            else lp.setMargins(m, m, m, m); // Corners
            
            h.setLayoutParams(lp);
            final int idx = i;
            h.setOnTouchListener(new ResizeTouchListener(idx));

            addView(h);
            handles[i] = h;
        }

        // Rotation handle
        rotateHandle = new ImageView(getContext());
        rotateHandle.setBackgroundResource(R.drawable.shape_circle);
        rotateHandle.setImageResource(android.R.drawable.ic_menu_rotate);
        rotateHandle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        rotateHandle.setPadding(8, 8, 8, 8);
        rotateHandle.setElevation(20f);
        rotateHandle.setVisibility(GONE);

        LayoutParams rlp = new LayoutParams(hPx + dp(12), hPx + dp(12),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        rlp.bottomMargin = p - dp(ROTATE_OFFSET_DP) - (hPx + dp(12));
        rotateHandle.setLayoutParams(rlp);
        rotateHandle.setOnTouchListener(new RotateTouchListener());
        addView(rotateHandle);
    }

    private int dp(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    // ─── Draw selection border ───────────────────────────────────────────────
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#4285F4"));
        borderPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (isSelected) {
            int p = dp(PADDING_DP);
            c.drawRect(p, p, getWidth() - p, getHeight() - p, borderPaint);
        }
    }

    // ─── Body drag + multi-touch ─────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        scaleGestureDetector.onTouchEvent(e);
        
        switch (e.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                if (!isSelected && selectionListener != null) {
                    selectionListener.onOverlaySelected(this);
                }
                dragDx = getX() - e.getRawX();
                dragDy = getY() - e.getRawY();
                isPinching = false;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() == 2) {
                    isPinching       = true;
                    lastPinchAngle   = angle(e);
                    startRotation    = getRotation();
                    startScale       = getScaleX();
                    if (content instanceof TextView)
                        startTextSize = ((TextView) content).getTextSize();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isPinching && e.getPointerCount() >= 2) {
                    // Rotate
                    float ang = angle(e);
                    setRotation(startRotation + (ang - lastPinchAngle));
                } else if (!isPinching) {
                    setX(e.getRawX() + dragDx);
                    setY(e.getRawY() + dragDy);
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                if (e.getPointerCount() <= 2) isPinching = false;
                if (e.getPointerCount() > 1) {
                    int rem = e.getActionIndex() == 0 ? 1 : 0;
                    float remRawX = e.getX(rem) + getX();
                    float remRawY = e.getY(rem) + getY();
                    dragDx = getX() - remRawX;
                    dragDy = getY() - remRawY;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPinching = false;
                return true;
        }
        return true;
    }

    private class DoubleTapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!isSelected && selectionListener != null) {
                selectionListener.onOverlaySelected(TransformableOverlay.this);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (content != null) {
                content.performClick();
            }
            return true;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            if (content instanceof TextView) {
                TextView tv = (TextView) content;
                float newSize = tv.getTextSize() * factor;
                newSize = Math.max(8f, Math.min(newSize, 800f));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
            } else {
                float newScale = getScaleX() * factor;
                newScale = Math.max(0.05f, Math.min(newScale, 20f));
                setScaleX(newScale);
                setScaleY(newScale);
            }
            return true;
        }
    }

    private void applyPinchScale(float newScale) {
        newScale = Math.max(0.1f, Math.min(newScale, 15f));
        if (content instanceof TextView) {
            float sz = Math.max(8f, Math.min(startTextSize * (newScale / startScale), 600f));
            ((TextView) content).setTextSize(TypedValue.COMPLEX_UNIT_PX, sz);
        } else {
            setScaleX(newScale);
            setScaleY(newScale);
        }
    }

    private float spacing(MotionEvent e) {
        float dx = e.getX(0) - e.getX(1);
        float dy = e.getY(0) - e.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float angle(MotionEvent e) {
        return (float) Math.toDegrees(Math.atan2(e.getY(0) - e.getY(1), e.getX(0) - e.getX(1)));
    }

    // ─── Resize Touch Listener ──────────────────────────────────────────────
    private class ResizeTouchListener implements OnTouchListener {
        private final int handleIdx; 
        private float startRawX, startRawY;
        private float startTextSz;
        private float startSc;
        private int startWidth, startHeight;

        ResizeTouchListener(int idx) { this.handleIdx = idx; }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRawX  = ev.getRawX();
                    startRawY  = ev.getRawY();
                    startSc    = getScaleX();
                    startWidth = content.getWidth();
                    if (content instanceof TextView)
                        startTextSz = ((TextView) content).getTextSize();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getRawX() - startRawX;
                    float dy = ev.getRawY() - startRawY;

                    float signX = 0, signY = 0;
                    if (handleIdx == 0) { signX = -1; signY = -1; } // TL
                    else if (handleIdx == 1) { signX = 1; signY = -1; } // TR
                    else if (handleIdx == 2) { signX = -1; signY = 1; } // BL
                    else if (handleIdx == 3) { signX = 1; signY = 1; } // BR
                    else if (handleIdx == 4) { signY = -1; } // Top Center
                    else if (handleIdx == 5) { signY = 1; } // Bottom Center
                    else if (handleIdx == 6) { signX = -1; } // Left Center
                    else if (handleIdx == 7) { signX = 1; } // Right Center

                    float delta = (signX * dx + signY * dy);
                    if (signX != 0 && signY != 0) delta /= 1.4f; // Normalize diagonal

                    if (content instanceof TextView) {
                        float newSize = Math.max(8f, startTextSz + delta * 0.8f);
                        ((TextView) content).setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
                    } else {
                        float factor = 1f + (delta / 200f);
                        float newScale = Math.max(0.05f, Math.min(startSc * factor, 20f));
                        setScaleX(newScale);
                        setScaleY(newScale);
                    }
                    return true;
            }
            return false;
        }
    }

    // ─── Rotation Touch Listener ─────────────────────────────────────────────
    private class RotateTouchListener implements OnTouchListener {
        private float startAngleOffset;

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            // Centre of overlay in parent (canvas) coordinates
            float cx = getX() + getWidth()  * 0.5f;
            float cy = getY() + getHeight() * 0.5f;

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    float touchAngle = (float) Math.toDegrees(
                            Math.atan2(ev.getRawY() - cy, ev.getRawX() - cx));
                    startAngleOffset = getRotation() - touchAngle;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float curAngle = (float) Math.toDegrees(
                            Math.atan2(ev.getRawY() - cy, ev.getRawX() - cx));
                    setRotation(startAngleOffset + curAngle);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        }
    }
}
