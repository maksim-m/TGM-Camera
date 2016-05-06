package me.maxdev.tgmcamera.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * Created by Max on 29.04.2016.
 */
public class OrientationChangeListener extends OrientationEventListener {

    public static final String BROADCAST_ORIENTATION_CHANGED = "OrientationChanged";
    public static final String EXTRA_NEW_ORIENTATION_KEY = "NewOrientation";
    public static final int ORIENTATION_NONE = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT_FLIPPED = 3;
    public static final int ORIENTATION_LANDSCAPE_FLIPPED = 4;

    private Context context;
    private int previousOrientation = ORIENTATION_NONE;

    public OrientationChangeListener(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == ORIENTATION_UNKNOWN) {
            return;
        }

        int newOrientation = ORIENTATION_NONE;
        if (orientation >= 0 && orientation < 45 || orientation >= 315 && orientation <= 360) {
            newOrientation = ORIENTATION_PORTRAIT;
        } else if (orientation >= 45 && orientation < 135) {
            newOrientation = ORIENTATION_LANDSCAPE;
        } else if (orientation >= 135 && orientation < 225) {
            newOrientation = ORIENTATION_PORTRAIT_FLIPPED;
        } else if (orientation >= 225 && orientation < 315) {
            newOrientation = ORIENTATION_LANDSCAPE_FLIPPED;
        } else {
            Log.e("ooo", orientation + " WRONG ORIENTATION");
        }
        if (newOrientation != ORIENTATION_NONE && newOrientation != previousOrientation) {
            previousOrientation = newOrientation;
            sendOrientationChangedBroadcast(newOrientation);
        }
    }

    private void sendOrientationChangedBroadcast(int newOrientation) {
        Intent intent = new Intent(BROADCAST_ORIENTATION_CHANGED);
        intent.putExtra(EXTRA_NEW_ORIENTATION_KEY, newOrientation);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
