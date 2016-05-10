package me.maxdev.tgmcamera.util;

import java.io.File;

/**
 * Created by Max on 28.04.2016.
 */
public interface OnCaptureFinishedListener {
    void onSuccess(File file);
    void onError();
}
