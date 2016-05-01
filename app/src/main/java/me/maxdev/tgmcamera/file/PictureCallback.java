package me.maxdev.tgmcamera.file;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.maxdev.tgmcamera.util.OnCaptureFinishedListener;
import me.maxdev.tgmcamera.R;

/**
 * Created by Max on 28.04.2016.
 */
public class PictureCallback implements Camera.PictureCallback {



    private static final String LOG_TAG = "PictureCallback";

    private Context context;
    private OnCaptureFinishedListener listener;

    public PictureCallback(Context context, OnCaptureFinishedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = OutputFileHelper.getOutputMediaFile(context,
                OutputFileHelper.MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.e(LOG_TAG, "Can not create output file.");
            // TODO
            listener.onError();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(LOG_TAG, "File not found: " + e.getMessage());
            listener.onError();
            return;
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
            listener.onError();
            return;
        }
        Log.d(LOG_TAG, "OK");
        listener.onSuccess();
        addToGallery(pictureFile);
    }

    /**
     * Create a File for saving an image or video
     */


    private void addToGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }



}