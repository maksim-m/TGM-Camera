package me.maxdev.tgmcamera.util;

import java.io.File;

public interface OnCaptureFinishedListener {
    void onSuccess(File file);
    void onError();
}
