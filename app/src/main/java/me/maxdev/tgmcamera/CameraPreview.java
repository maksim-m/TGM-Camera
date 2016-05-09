package me.maxdev.tgmcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import me.maxdev.tgmcamera.util.OrientationChangeListener;

/**
 * Created by Max on 28.04.2016.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = "CameraPreview";
    private static final float DEFAULT_ASPECT_RATIO = 4f / 3f;

    private Context context;
    private Camera camera;
    private CameraIdProvider cameraIdProvider;
    private SurfaceHolder surfaceHolder;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera.Size previewSize;
    private float aspectRatio;
    private List<Camera.Size> supportedPictureSizes;

    public CameraPreview(Context context, Camera camera, CameraIdProvider cameraIdProvider, float aspectRatio) {
        super(context);
        this.context = context;
        this.aspectRatio = aspectRatio;
        this.camera = camera;
        this.cameraIdProvider = cameraIdProvider;
        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            supportedPictureSizes = parameters.getSupportedPictureSizes();
        }
    }

    public void setAspectRatio(float aspectRatio) {
        Log.e("xxx", "new aspect ratio = " + aspectRatio);
        this.aspectRatio = aspectRatio;
        requestLayout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // TODO: set preview size and make any resize, rotate or reformatting changes here
        /*
        If you want to set a specific size for your camera preview, set this in the surfaceChanged()
        method as noted in the comments above. When setting preview size, you must use values
        from getSupportedPreviewSizes(). Do not set arbitrary values in the setPreviewSize() method.
         */
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            //requestLayout();
            Camera.Size pictureSize = getMaximalPictureSizeWithSameAspectRatio();
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            camera.setParameters(parameters);
        }

        Log.e("xxx", "surfaceChanged: width=" + width + " height=" + height);

        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            Log.d(LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /*final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);/*/

        int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int width = (int) (height * aspectRatio);

        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(width, height);
        }
        setMeasuredDimension(width, height);
        Log.e("xxx", "onMeasure: width=" + width + " height=" + height);
    }

    private Camera.Size getOptimalPreviewSize(int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (supportedPreviewSizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Find size
        for (Camera.Size size : supportedPreviewSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : supportedPreviewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private Camera.Size getMaximalPictureSizeWithSameAspectRatio() {
        Camera.Size pictureSize = supportedPictureSizes.get(0);
        for (Camera.Size size : supportedPictureSizes) {
            Log.e("xxx", size.width + " x " + size.height);
            int resultArea = pictureSize.width * pictureSize.height;
            int newArea = size.width * size.height;
            if (newArea >= resultArea &&
                    (float) size.width / size.height == (float) previewSize.width / previewSize.height) {
                pictureSize = size;
            }
        }
        Log.e("xxx", "Result: " + pictureSize.width + " x " + pictureSize.height);
        return pictureSize;
    }

    public void updateCameraOrientation(int newOrientation) {
        int rotation = getOutputMediaRotation(newOrientation);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(rotation);
        camera.setParameters(parameters);
    }

    public int getOutputMediaRotation(int orientation) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraIdProvider.getCurrentCameraId(), info);
        Log.d(LOG_TAG, "updateCameraOrientation() newOrientation == " + orientation);
        Log.d(LOG_TAG, "info.orientation == " + info.orientation);
        int degrees = 0;

        /*int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        Log.d(LOG_TAG, "rotation == " + rotation);
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }*/
        switch (orientation) {
            case OrientationChangeListener.ORIENTATION_NONE:
                degrees = 0;
                break;
            case OrientationChangeListener.ORIENTATION_PORTRAIT:
                degrees = 0;
                break;
            case OrientationChangeListener.ORIENTATION_LANDSCAPE:
                degrees = 270;
                break;
            case OrientationChangeListener.ORIENTATION_PORTRAIT_FLIPPED:
                degrees = 180;
                break;
            case OrientationChangeListener.ORIENTATION_LANDSCAPE_FLIPPED:
                degrees = 90;
                break;

        }
        Log.d(LOG_TAG, "degrees == " + degrees);

        int result;
        if (cameraIdProvider.getCurrentCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(LOG_TAG, "result == " + result);
        return result;
    }

    public interface CameraIdProvider {
        int getCurrentCameraId();
    }
}
