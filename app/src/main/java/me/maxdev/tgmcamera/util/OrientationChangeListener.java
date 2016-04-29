package me.maxdev.tgmcamera.util;

import android.content.Context;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * Created by Max on 29.04.2016.
 */
public class OrientationChangeListener extends OrientationEventListener {

    public static final int ORIENTATION_NONE = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT_FLIPPED = 3;
    public static final int ORIENTATION_LANDSCAPE_FLIPPED = 4;

    private int previousOrientation = ORIENTATION_NONE;

    public OrientationChangeListener(Context context) {
        super(context);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == ORIENTATION_UNKNOWN) {
            return;
        }

        int newOrientation = ORIENTATION_NONE;
        if (orientation >= 0 && orientation < 45 || orientation >= 315 && orientation <= 360) {
            Log.e("ooo", orientation + " Vertical up");
            newOrientation = ORIENTATION_PORTRAIT;
        } else if (orientation >= 45 && orientation < 135) {
            Log.e("ooo", orientation + " Horizontal right");
            newOrientation = ORIENTATION_LANDSCAPE;
        } else if (orientation >= 135 && orientation < 225) {
            Log.e("ooo", orientation + " Vertical down");
            newOrientation = ORIENTATION_PORTRAIT_FLIPPED;
        } else if (orientation >= 225 && orientation < 315) {
            Log.e("ooo", orientation + " Horizontal left");
            newOrientation = ORIENTATION_LANDSCAPE_FLIPPED;
        } else {
            Log.e("ooo", orientation + " WRONG ORIENTATION");
        }
        if (newOrientation != ORIENTATION_NONE && newOrientation != previousOrientation) {
            previousOrientation = newOrientation;
            // TODO callback(newOrientation)
        }
    }
}
