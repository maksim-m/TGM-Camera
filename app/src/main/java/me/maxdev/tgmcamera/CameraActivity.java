package me.maxdev.tgmcamera;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.maxdev.tgmcamera.file.PictureCallback;
import me.maxdev.tgmcamera.util.OnCaptureFinishedListener;
import me.maxdev.tgmcamera.util.OrientationChangeListener;

public class CameraActivity extends AppCompatActivity implements
        View.OnSystemUiVisibilityChangeListener, OnCaptureFinishedListener {

    private static final String LOG_TAG = "CameraActivity";

    @BindView(R.id.layout_root)
    FrameLayout rootLayout;
    @BindView(R.id.layout_preview)
    FrameLayout previewLayout;

    private Camera camera;
    private CameraPreview cameraPreview;
    private boolean readyForCapture = false;
    private OrientationChangeListener orientationChangeListener;

    private Runnable systemUiHider = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Start systemUiHider.");
            hideSystemUi();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        rootLayout.setOnSystemUiVisibilityChangeListener(this);
        hideSystemUi();
        orientationChangeListener = new OrientationChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCameraPreview();
        readyForCapture = true;
        hideSystemUi();
        orientationChangeListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO releaseMediaRecorder();
        releaseCamera();
        orientationChangeListener.disable();
    }


    private void initCameraPreview() {
        camera = getCameraInstance();
        if (camera == null) {
            // TODO: show error dialog
        } else {
            cameraPreview = new CameraPreview(this, camera);

            previewLayout.addView(cameraPreview);
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            cameraPreview.getHolder().removeCallback(cameraPreview);
            camera = null;
        }

    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(0); // reset all flags
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @OnClick(R.id.button_take_picture)
    void onTakePictureClicked() {
        if (readyForCapture) {
            final PictureCallback pictureCallback = new PictureCallback(this, this);
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.e("xxx", "onAutoFocus");
                    camera.takePicture(null, null, pictureCallback);
                    readyForCapture = false;
                }
            });
        } else {
            Log.w(LOG_TAG, "Camera not ready yet.");
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        rootLayout.getHandler().postDelayed(systemUiHider, 2000);
    }

    public static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            // TODO
        }
        return camera;
    }

    @Override
    public void onSuccess() {
        camera.startPreview();
        readyForCapture = true;
    }

    @Override
    public void onError() {
        // TODO
        readyForCapture = true;
    }

}
