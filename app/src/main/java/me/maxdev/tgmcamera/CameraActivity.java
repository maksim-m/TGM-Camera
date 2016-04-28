package me.maxdev.tgmcamera;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraActivity extends AppCompatActivity implements View.OnSystemUiVisibilityChangeListener {

    private static final String LOG_TAG = "CameraActivity";

    @BindView(R.id.root_layout)
    FrameLayout rootLayout;
    @BindView(R.id.preview_layout)
    FrameLayout previewLayout;

    private Camera camera;
    private CameraPreview cameraPreview;

    private Runnable systemUiHider = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Start systemUiHider.");
            hideStatusUi();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        rootLayout.setOnSystemUiVisibilityChangeListener(this);
        hideStatusUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCameraPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO releaseMediaRecorder();
        releaseCamera();
    }

    private void initCameraPreview() {
        camera = getCameraInstance();
        if (camera == null) {
            // TODO: show error dialog
        } else {
            cameraPreview = new CameraPreview(this, camera);
            //cameraPreview.updateCameraParams(camera);
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

    private void hideStatusUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(0); // reset all flags
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
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

}
