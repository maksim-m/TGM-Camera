package me.maxdev.tgmcamera.media;

import android.content.Context;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.maxdev.tgmcamera.util.OnCaptureFinishedListener;

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
        new SavePhotoTask().execute(data);
    }

    private class SavePhotoTask extends AsyncTask<byte[], Void, File> {
        @Override
        protected File doInBackground(byte[]... data) {
            final File pictureFile = OutputFileHelper.getOutputMediaFile(context,
                    OutputFileHelper.MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.e(LOG_TAG, "Can not create output file.");
                // TODO
                listener.onError();
                return null;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data[0]);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found: " + e.getMessage());
                listener.onError();
                return null;
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
                listener.onError();
                return null;
            }
            Log.d(LOG_TAG, "OK");
            GalleryHelper.addToGallery(context, pictureFile);

            return pictureFile;
        }

        @Override
        protected void onPostExecute(File file) {
            listener.onSuccess(file);
        }
    }

}