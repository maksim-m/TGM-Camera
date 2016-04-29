package me.maxdev.tgmcamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Max on 29.04.2016.
 */
public class ShutterButton extends ImageView {

    private int weight;
    private int height;
    private int color;
    private Paint innerBlueCirclePaint;
    private Paint outerBlueCirclePaint;
    private Paint outerWhiteCirclePaint;

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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        weight = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(weight / 2, height / 2, Math.min(weight, height) / 2, innerBlueCirclePaint);
        canvas.drawCircle(weight / 2, height / 2, Math.min(weight, height) / 2, outerBlueCirclePaint);
    }

    private void initShutterButton(Context context, AttributeSet attrs) {
        setFocusable(true);
        setClickable(true);
        setScaleType(ScaleType.CENTER_INSIDE);

        innerBlueCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBlueCirclePaint.setStyle(Paint.Style.FILL);
        innerBlueCirclePaint.setColor(Color.BLUE);

        outerBlueCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerBlueCirclePaint.setStyle(Paint.Style.STROKE);
        outerBlueCirclePaint.setColor(Color.GREEN);
    }
}
