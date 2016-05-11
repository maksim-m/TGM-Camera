package me.maxdev.tgmcamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import me.maxdev.tgmcamera.R;

/**
 * Created by Max on 29.04.2016.
 */
public class ShutterButton extends View {

    private static final String LOG_TAG = "ShutterButton";

    private static final int MODE_PHOTO = 1;
    private static final int MODE_VIDEO = 2;
    private static final int MODE_VIDEO_RECORD = 3;

    private int width;
    private int height;
    private int cx, cy;
    private float radius;
    private float outerBlueCircleRadius;
    private float innerBlueCircleRadius;
    private int currentMode;
    private Paint innerBlueCirclePaint;
    private Paint outerBlueCirclePaint;
    private Paint whiteCirclePaint;
    private Paint recordCirclePaint;

    private Runnable captureAnimationIn = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;
            //long now = AnimationUtils.currentAnimationTimeMillis();

            outerBlueCircleRadius -= 10;
            if (outerBlueCircleRadius >= innerBlueCircleRadius) {
                needNewFrame = true;
            }

            if (needNewFrame) {
                postDelayed(this, 2);
            } else {
                Log.d(LOG_TAG, "captureAnimationIn done.");
                postDelayed(captureAnimationOut, 100);
            }
            invalidate();
        }
    };
    private Runnable captureAnimationOut = new Runnable() {
        @Override
        public void run() {
            boolean needNewFrame = false;

            outerBlueCircleRadius += 10;
            if (outerBlueCircleRadius >= radius - 12) {
                outerBlueCircleRadius = radius - 12;
            }
            if (outerBlueCircleRadius != radius - 12) {
                needNewFrame = true;
            }

            if (needNewFrame) {
                postDelayed(this, 7);
            } else {
                Log.d(LOG_TAG, "captureAnimationOut done.");
                removeCallbacks(captureAnimationIn);
                removeCallbacks(captureAnimationOut);
            }
            invalidate();
        }
    };
    private Rect square;
    private RectF squareF;

    public ShutterButton(Context context) {
        super(context);
        initShutterButton(context, null);
    }

    public ShutterButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initShutterButton(context, attrs);
    }

    public ShutterButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initShutterButton(context, attrs);
    }

    public void animateCapture() {
        if (currentMode == MODE_PHOTO) {
            removeCallbacks(captureAnimationIn);
            removeCallbacks(captureAnimationOut);
            post(captureAnimationIn);
        }
    }

    public void startPhotoMode() {
        currentMode = MODE_PHOTO;
        invalidate();
    }

    public void startVideoMode() {
        currentMode = MODE_VIDEO;
        invalidate();
    }

    public void startVideoRecordingMode() {
        currentMode = MODE_VIDEO_RECORD;
        invalidate();
    }

    public void stopVideoRecordingMode() {
        currentMode = MODE_VIDEO;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        cx = width / 2;
        cy = height / 2;
        radius = Math.min(width, height) / 2.0f;
        outerBlueCircleRadius = radius - 12;
        innerBlueCircleRadius = radius - 42;
        square = new Rect(60, 60, width - 60, height - 60);
        squareF = new RectF(square);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (currentMode) {
            case MODE_PHOTO:
                canvas.drawCircle(cx, cy, radius, whiteCirclePaint);
                canvas.drawCircle(cx, cy, outerBlueCircleRadius, outerBlueCirclePaint);
                canvas.drawCircle(cx, cy, innerBlueCircleRadius, innerBlueCirclePaint);
                break;
            case MODE_VIDEO:
                canvas.drawCircle(cx, cy, radius, whiteCirclePaint);
                canvas.drawCircle(cx, cy, radius - 70, recordCirclePaint);
                break;
            case MODE_VIDEO_RECORD:
                canvas.drawCircle(cx, cy, radius, recordCirclePaint);
                canvas.drawRoundRect(squareF, 10, 10, whiteCirclePaint);
                break;
        }

    }

    private void initShutterButton(Context context, AttributeSet attrs) {
        currentMode = MODE_PHOTO;
        setFocusable(true);
        setClickable(true);
        //setScaleType(ScaleType.CENTER_INSIDE);

        whiteCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteCirclePaint.setStyle(Paint.Style.FILL);
        whiteCirclePaint.setColor(context.getResources().getColor(R.color.shutterButtonCornerColor));

        innerBlueCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBlueCirclePaint.setStyle(Paint.Style.FILL);
        innerBlueCirclePaint.setColor(context.getResources().getColor(R.color.shutterButtonInnerCircleColor));

        outerBlueCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerBlueCirclePaint.setStyle(Paint.Style.FILL);
        outerBlueCirclePaint.setColor(context.getResources().getColor(R.color.shutterButtonOuterCircleColor));

        recordCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordCirclePaint.setStyle(Paint.Style.FILL);
        recordCirclePaint.setColor(context.getResources().getColor(R.color.shutterButtonRecordCircleColor));
    }
}
