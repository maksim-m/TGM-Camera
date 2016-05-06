package me.maxdev.tgmcamera.media;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;

/**
 * Created by Max on 06.05.2016.
 */
public class GalleryHelper {

    private static final String LOG_TAG = "GalleryHelper";

    private GalleryHelper() {
    }

    public static void addToGallery(Context context, File file) {
        Log.d(LOG_TAG, "addToGallery file: " + file.getPath());
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * @param context  Context
     * @param fileName relative file name (e.g. VID_123.mp4) which DOESN'T contains a full path
     */
    public static void addToGallery(Context context, String fileName) {
        Log.d(LOG_TAG, "addToGallery fileName: " + fileName);
        File file = new File(OutputFileHelper.getOutputDir(context), fileName);
        addToGallery(context, file);
    }
}
