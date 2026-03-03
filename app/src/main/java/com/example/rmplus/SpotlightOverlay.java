package com.example.rmplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Overlay view that highlights a target view with a dark surround
 * and shows a tooltip card with Next / Skip buttons.
 */
public class SpotlightOverlay extends FrameLayout {

    private final Paint overlayPaint;
    private final Paint clearPaint;
    private final ArrayList<SpotlightTarget> targets;
    private int currentIndex = 0;

    private float spotX, spotY, spotRadius;
    private float animatedRadius = 0f;

    private View tooltipView;
    private OnFinishListener finishListener;

    public interface OnFinishListener {
        void onFinish();
    }

    public SpotlightOverlay(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(true); // block clicks through

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#CC000000")); // dark overlay
        overlayPaint.setStyle(Paint.Style.FILL);

        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        targets = new ArrayList<>();
    }

    public void addTarget(View view, String title, String description) {
        targets.add(new SpotlightTarget(view, title, description));
    }

    public void setOnFinishListener(OnFinishListener listener) {
        this.finishListener = listener;
    }

    public void start(Activity activity) {
        if (targets.isEmpty())
            return;

        // Add to the activity's root content view
        ViewGroup root = activity.findViewById(android.R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        root.addView(this, params);

        // Use hardware layer for PorterDuff to work
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Show first target after a brief delay for layout
        postDelayed(() -> showTarget(0), 300);
    }

    private void showTarget(int index) {
        if (index >= targets.size()) {
            dismiss();
            return;
        }

        currentIndex = index;
        SpotlightTarget target = targets.get(index);

        // Get target position on screen
        int[] loc = new int[2];
        target.view.getLocationOnScreen(loc);

        int[] myLoc = new int[2];
        getLocationOnScreen(myLoc);

        float targetCenterX = loc[0] - myLoc[0] + target.view.getWidth() / 2f;
        float targetCenterY = loc[1] - myLoc[1] + target.view.getHeight() / 2f;
        float targetRadius = Math.max(target.view.getWidth(), target.view.getHeight()) / 2f + dpToPx(16);

        spotX = targetCenterX;
        spotY = targetCenterY;
        spotRadius = targetRadius;

        // Animate circle opening
        animateSpotlight(targetRadius);

        // Show tooltip card
        showTooltip(target, targetCenterX, targetCenterY, targetRadius);
    }

    private void animateSpotlight(float targetRadius) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, targetRadius);
        animator.setDuration(400);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            animatedRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw dark overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
        // Clear the spotlight circle
        if (animatedRadius > 0) {
            canvas.drawCircle(spotX, spotY, animatedRadius, clearPaint);
        }
    }

    private void showTooltip(SpotlightTarget target, float cx, float cy, float radius) {
        // Remove old tooltip if exists
        if (tooltipView != null) {
            removeView(tooltipView);
        }

        tooltipView = LayoutInflater.from(getContext())
                .inflate(R.layout.tooltip_spotlight, this, false);

        TextView txtStep = tooltipView.findViewById(R.id.txtStepCounter);
        TextView txtTitle = tooltipView.findViewById(R.id.txtSpotTitle);
        TextView txtDesc = tooltipView.findViewById(R.id.txtSpotDesc);
        TextView btnSkip = tooltipView.findViewById(R.id.btnSpotSkip);
        TextView btnNext = tooltipView.findViewById(R.id.btnSpotNext);

        txtStep.setText((currentIndex + 1) + "/" + targets.size());
        txtTitle.setText(target.title);
        txtDesc.setText(target.description);

        // Last step → show "Got it" instead of "Next"
        if (currentIndex == targets.size() - 1) {
            btnNext.setText(R.string.onboard_got_it);
            btnSkip.setVisibility(GONE);
        } else {
            btnNext.setText(R.string.onboard_next);
            btnSkip.setVisibility(VISIBLE);
        }

        btnNext.setOnClickListener(v -> {
            showTarget(currentIndex + 1);
        });

        btnSkip.setOnClickListener(v -> {
            dismiss();
        });

        // Add tooltip with proper positioning
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        // Determine if tooltip goes above or below the target
        float screenMidY = getHeight() / 2f;
        int margin = dpToPx(16);

        if (cy < screenMidY) {
            // Target is in top half → show tooltip BELOW
            lp.topMargin = (int) (cy + radius + dpToPx(12));
        } else {
            // Target is in bottom half → show tooltip ABOVE
            // We need to measure the tooltip first
            tooltipView.measure(
                    MeasureSpec.makeMeasureSpec(getWidth() - dpToPx(32), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
            int tooltipHeight = tooltipView.getMeasuredHeight();
            lp.topMargin = (int) (cy - radius - tooltipHeight - dpToPx(12));
            if (lp.topMargin < dpToPx(24)) {
                lp.topMargin = dpToPx(24);
            }
        }

        // Center horizontally with padding
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.leftMargin = margin;
        lp.rightMargin = margin;

        addView(tooltipView, lp);

        // Animate tooltip entry
        tooltipView.setAlpha(0f);
        tooltipView.setTranslationY(20f);
        tooltipView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void dismiss() {
        // Mark onboarding as done
        SharedPreferences sp = getContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        sp.edit().putBoolean("onboarding_done", true).apply();

        // Fade out animation
        animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewGroup parent = (ViewGroup) getParent();
                        if (parent != null) {
                            parent.removeView(SpotlightOverlay.this);
                        }
                        if (finishListener != null) {
                            finishListener.onFinish();
                        }
                    }
                })
                .start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ===== Target Data =====
    static class SpotlightTarget {
        View view;
        String title;
        String description;

        SpotlightTarget(View view, String title, String description) {
            this.view = view;
            this.title = title;
            this.description = description;
        }
    }
}
