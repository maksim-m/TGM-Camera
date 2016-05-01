package me.maxdev.tgmcamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.maxdev.tgmcamera.file.OutputFileHelper;
import me.maxdev.tgmcamera.file.PictureCallback;
import me.maxdev.tgmcamera.util.OnCaptureFinishedListener;
import me.maxdev.tgmcamera.util.OrientationChangeListener;

public class CameraActivity extends AppCompatActivity implements
        View.OnSystemUiVisibilityChangeListener, OnCaptureFinishedListener {

    private static final String LOG_TAG = "CameraActivity";

    private static final int MODE_PHOTO = 0;
    private static final int MODE_VIDEO = 1;

    private static final int shortAnimationDuration = 400;

    @BindView(R.id.layout_root)
    FrameLayout rootLayout;
    @BindView(R.id.layout_preview)
    FrameLayout previewLayout;
    @BindView(R.id.blocking_toolbar)
    View blockingToolbar;
    @BindView(R.id.toolbar)
    RelativeLayout toolbar;

    private Camera camera;
    private CameraPreview cameraPreview;
    private MediaRecorder mediaRecorder;
    private OrientationChangeListener orientationChangeListener;
    private int currentMode = MODE_PHOTO;
    private boolean readyForCapture = false;
    private boolean isRecording = false;
    private int toolbarHeight;

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
        initCameraPreview(currentMode);
        readyForCapture = true;
        hideSystemUi();
        orientationChangeListener.enable();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (blockingToolbar.getVisibility() != View.GONE) {
            toolbarHeight = blockingToolbar.getMeasuredWidth();
        }
        toolbar.setMinimumWidth(toolbarHeight);
        toolbar.requestLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO releaseMediaRecorder();
        releaseCamera();
        orientationChangeListener.disable();
    }


    private void initCameraPreview(int newMode) {
        camera = getCameraInstance();
        if (camera == null) {
            // TODO: show error dialog
        } else {
            float aspectRatio = newMode == MODE_PHOTO ? 4f / 3f : 16f / 9f;
            cameraPreview = new CameraPreview(this, camera, aspectRatio);
            previewLayout.addView(cameraPreview);
        }
    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(0); // reset all flags
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @OnClick(R.id.button_take_picture)
    void onTakePictureClicked() {
        if (readyForCapture) {
            if (currentMode == MODE_PHOTO) {
                final PictureCallback pictureCallback = new PictureCallback(this, this);
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        Log.e("xxx", "onAutoFocus");
                        camera.takePicture(null, null, pictureCallback);
                        readyForCapture = false;
                    }
                });
            } else if (currentMode == MODE_VIDEO) {
                if (isRecording) {
                    // stop recording and release camera
                    mediaRecorder.stop();  // stop the recording
                    releaseMediaRecorder(); // release the MediaRecorder object
                    camera.lock();         // take camera access back from MediaRecorder

                    // TODO inform the user that recording has stopped

                    isRecording = false;
                } else {
                    // initialize video camera
                    if (prepareVideoRecorder()) {
                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                        mediaRecorder.start();

                        // TODO inform the user that recording has started

                        isRecording = true;
                    } else {
                        Log.e(LOG_TAG, "Failed to prepare MediaRecorder.");
                        // prepare didn't work, release the camera
                        releaseMediaRecorder();
                        // TODO inform user
                    }

                }
            }
        } else {
            Log.w(LOG_TAG, "Camera not ready yet.");
        }
    }

    @OnClick(R.id.button_ok)
    void onOkButtonClicked() {
        changeCurrentMode();
    }

    private void changeCurrentMode() {
        final int newMode = currentMode == MODE_PHOTO ? MODE_VIDEO : MODE_PHOTO;
        final float aspectRatio = newMode == MODE_PHOTO ? 4f / 3f : 16f / 9f;

        // update toolbar
        if (newMode == MODE_VIDEO) {

            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbar));
            //releaseCamera();
            blockingToolbar.setVisibility(View.GONE);
            cameraPreview.setAspectRatio(aspectRatio);
            //resetCameraPreviews();
            //initCameraPreview(newMode);
            //toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarTransparent));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                            getResources().getColor(R.color.colorToolbar),
                            getResources().getColor(R.color.colorToolbarTransparent));
                    colorAnimation.setDuration(shortAnimationDuration); // milliseconds
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            toolbar.setBackgroundColor((int) animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();
                }
            }, 300);

        } else if (newMode == MODE_PHOTO) {
            //releaseCamera();
            //resetCameraPreviews();
            blockingToolbar.setAlpha(0f);
            blockingToolbar.setVisibility(View.VISIBLE);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                    getResources().getColor(R.color.colorToolbarTransparent),
                    getResources().getColor(R.color.colorToolbar));
            colorAnimation.setDuration(shortAnimationDuration); // milliseconds
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    toolbar.setBackgroundColor((int) animator.getAnimatedValue());
                }
            });
            colorAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    cameraPreview.setAspectRatio(aspectRatio);
                    blockingToolbar.setAlpha(1f);
                    toolbar.setBackgroundColor(getResources().getColor(R.color.colorTransparent));
                }
            });
            colorAnimation.start();
        }

        currentMode = newMode;
    }


    private void resetCameraPreviews() {
        previewLayout.removeView(cameraPreview);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        rootLayout.getHandler().postDelayed(systemUiHider, 2000);
    }

    public static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            // TODO
        }
        return camera;
    }

    private boolean prepareVideoRecorder(){

        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mediaRecorder.setOutputFile(OutputFileHelper.getOutputMediaFile(this,
                OutputFileHelper.MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(LOG_TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            cameraPreview.getHolder().removeCallback(cameraPreview);
            camera = null;
        }
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
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
