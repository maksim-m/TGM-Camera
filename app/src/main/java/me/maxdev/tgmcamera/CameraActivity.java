package me.maxdev.tgmcamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.maxdev.tgmcamera.media.GalleryHelper;
import me.maxdev.tgmcamera.media.OutputFileHelper;
import me.maxdev.tgmcamera.media.PictureCallback;
import me.maxdev.tgmcamera.media.VideoFileObserver;
import me.maxdev.tgmcamera.util.OnCaptureFinishedListener;
import me.maxdev.tgmcamera.util.OnSwipeTouchListener;
import me.maxdev.tgmcamera.util.OrientationChangeListener;
import me.maxdev.tgmcamera.widget.ShutterButton;

public class CameraActivity extends AppCompatActivity implements
        View.OnSystemUiVisibilityChangeListener, OnCaptureFinishedListener, CameraPreview.CameraIdProvider {

    private static final String LOG_TAG = "CameraActivity";

    private static final int MODE_PHOTO = 0;
    private static final int MODE_VIDEO = 1;

    private static final int SHORT_ANIMATION_DURATION_IN_MILLIS = 300;
    private static VideoFileObserver videoFileObserver;

    @BindView(R.id.layout_root)
    FrameLayout rootLayout;
    @BindView(R.id.layout_preview)
    FrameLayout previewLayout;
    @BindView(R.id.toolbar)
    RelativeLayout toolbar;
    @BindView(R.id.button_switch_camera)
    ImageView switchCameraButton;
    @BindView(R.id.button_ok)
    ImageButton okButton;
    @BindView(R.id.button_cancel)
    ImageButton cancelButton;
    @BindView(R.id.button_shutter)
    ShutterButton shutterButton;

    private Camera camera;
    private CameraPreview cameraPreview;
    private MediaRecorder mediaRecorder;
    private OrientationChangeListener orientationChangeListener;
    private int currentMode = MODE_PHOTO;
    private int currentOrientation = OrientationChangeListener.ORIENTATION_PORTRAIT;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean autoFocusSupported = false;
    private boolean hasFrontCamera = false;
    private boolean cameraReady = false;
    private boolean isRecording = false;
    private Runnable systemUiHider = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Start systemUiHider.");
            hideSystemUi();
        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VideoFileObserver.BROADCAST_WRITE_FINISHED)) {
                String fileName = intent.getStringExtra(VideoFileObserver.EXTRA_FILE_NAME_KEY);
                onVideoCaptured(fileName);
            } else if (intent.getAction().equals(OrientationChangeListener.BROADCAST_ORIENTATION_CHANGED)) {
                int newOrientation = intent.getIntExtra(OrientationChangeListener.EXTRA_NEW_ORIENTATION_KEY,
                        OrientationChangeListener.ORIENTATION_NONE);
                onOrientationChanged(newOrientation);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        rootLayout.setOnSystemUiVisibilityChangeListener(this);
        hideSystemUi();
        orientationChangeListener = new OrientationChangeListener(this);
        autoFocusSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        initSwitchCameraButton();
    }

    private void initGestureDetector() {
        if (cameraPreview == null) {
            Log.d(LOG_TAG, "initGestureDetector: cameraPreview not ready.");
            return;
        }
        cameraPreview.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeTop() {
                if (currentMode == MODE_VIDEO && !isRecording) {
                    changeCurrentMode();
                }
            }

            @Override
            public void onSwipeBottom() {
                if (currentMode == MODE_PHOTO) {
                    changeCurrentMode();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showCameraPreview();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VideoFileObserver.BROADCAST_WRITE_FINISHED);
        intentFilter.addAction(OrientationChangeListener.BROADCAST_ORIENTATION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void showCameraPreview() {
        initCameraPreview(currentMode);
        cameraReady = true;
        hideSystemUi();
        orientationChangeListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopVideoRecording();
        }
        releaseCamera();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        orientationChangeListener.disable();
    }


    private void initCameraPreview(int newMode) {
        camera = getCameraInstance();
        if (camera == null) {
            // TODO: show error dialog
        } else {
            //float aspectRatio = newMode == MODE_PHOTO ? 4f / 3f : 16f / 9f;
            cameraPreview = new CameraPreview(this, camera, this);
            cameraPreview.updateCameraOrientation(currentOrientation);
            previewLayout.addView(cameraPreview);
            initGestureDetector();
        }
    }

    private void initSwitchCameraButton() {
        hasFrontCamera = Camera.getNumberOfCameras() > 1;
        Log.d(LOG_TAG, "hasFrontCamera: " + hasFrontCamera);
        if (hasFrontCamera) {
            switchCameraButton.setVisibility(View.VISIBLE);
        } else {
            switchCameraButton.setVisibility(View.INVISIBLE);
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

    @OnClick(R.id.button_shutter)
    void onShutterButtonClicked() {
        if (cameraReady) {
            if (currentMode == MODE_PHOTO) {
                shutterButton.animateCapture();
                capturePhoto();
            } else if (currentMode == MODE_VIDEO) {
                if (isRecording) {
                    stopVideoRecording();
                } else {
                    startVideoRecording();
                }
            }
        } else {
            Log.d(LOG_TAG, "Camera not ready yet.");
        }
    }

    @OnClick(R.id.button_switch_camera)
    void onSwitchCameraButtonClicked() {
        releaseCamera();
        resetCameraPreviews();
        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        initCameraPreview(currentMode);
    }

    private void startVideoRecording() {

        // initialize video camera
        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mediaRecorder.start();

            // TODO inform the user that recording has started
            shutterButton.startVideoRecordingMode();
            makeInvisible(switchCameraButton);
            makeToolbarTransparent();

            isRecording = true;
        } else {
            Log.e(LOG_TAG, "Failed to prepare MediaRecorder.");
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // TODO inform user
        }
    }

    private void stopVideoRecording() {
        // stop recording and release camera
        if (mediaRecorder != null) {
            mediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder
        }

        // TODO inform the user that recording has stopped
        makeToolbarVisible();
        shutterButton.stopVideoRecordingMode();
        makeVisible(switchCameraButton);


        isRecording = false;
    }

    private void capturePhoto() {
        final PictureCallback pictureCallback = new PictureCallback(this, this);
        if (autoFocusSupported) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    camera.takePicture(null, null, pictureCallback);
                    cameraReady = false;
                }
            });
        } else {
            camera.takePicture(null, null, pictureCallback);
            cameraReady = false;
        }
    }

    private void changeCurrentMode() {
        final int newMode = currentMode == MODE_PHOTO ? MODE_VIDEO : MODE_PHOTO;
        //final float aspectRatio = newMode == MODE_PHOTO ? 4f / 3f : 16f / 9f;

        if (isRecording) {
            stopVideoRecording();
        }

        // update toolbar
        if (newMode == MODE_VIDEO) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                            getResources().getColor(R.color.colorToolbar),
                            getResources().getColor(R.color.colorToolbarTransparent));
                    colorAnimation.setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS);
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            toolbar.setBackgroundColor((int) animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();
                    shutterButton.startVideoMode();
                }
            }, 300);

        } else if (newMode == MODE_PHOTO) {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                    getResources().getColor(R.color.colorToolbarTransparent),
                    getResources().getColor(R.color.colorToolbar));
            colorAnimation.setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    toolbar.setBackgroundColor((int) animator.getAnimatedValue());
                }
            });
            colorAnimation.start();
            shutterButton.startPhotoMode();
        }

        currentMode = newMode;
    }

    private void onOrientationChanged(int newOrientation) {
        Log.d(LOG_TAG, "onOrientationChanged. New orientation == " + newOrientation);
        if (camera != null) {
            cameraPreview.updateCameraOrientation(newOrientation);
        }
        // TODO
    }

    private void onVideoCaptured(String fileName) {
        Log.d(LOG_TAG, "Video file " + fileName + " WRITE_DONE");
        videoFileObserver.stopWatching();
        videoFileObserver = null;
        GalleryHelper.addToGallery(this, fileName);
        // TODO after video captured
    }

    private void resetCameraPreviews() {
        previewLayout.removeView(cameraPreview);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        rootLayout.getHandler().postDelayed(systemUiHider, 2000);
    }

    public Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(currentCameraId);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            // TODO
        }
        return camera;
    }

    private boolean prepareVideoRecorder() {
        if (videoFileObserver != null) {
            Log.w(LOG_TAG, "prepareVideoRecorder(): Not ready yet (fileObserver is watching now).");
            return false;
        }
        cameraPreview.updateCameraOrientation(currentOrientation);
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
        File outputFile = OutputFileHelper.getOutputMediaFile(this, OutputFileHelper.MEDIA_TYPE_VIDEO);
        if (outputFile == null) {
            Log.d(LOG_TAG, "failed to create output file");
            return false;
        }
        videoFileObserver = new VideoFileObserver(this, OutputFileHelper.getOutputDir(this).getPath(),
                FileObserver.CLOSE_WRITE);
        videoFileObserver.startWatching();
        mediaRecorder.setOutputFile(outputFile.toString());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

        mediaRecorder.setOrientationHint(cameraPreview.getOutputMediaRotation(currentOrientation));

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

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    @Override
    public void onSuccess(File file) {
        Log.d(LOG_TAG, "onSuccess(). file == " + file);

        camera.startPreview();
        cameraReady = true;
    }

    @Override
    public void onError() {
        // TODO
        cameraReady = true;
    }

    private void makeInvisible(View view) {
        final View v = view;
        v.animate()
                .alpha(0f)
                .setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void makeVisible(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS)
                .setListener(null);

    }

    private void makeToolbarTransparent() {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                getResources().getColor(R.color.colorToolbarTransparent),
                getResources().getColor(R.color.colorTransparent));
        colorAnimation.setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                toolbar.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    private void makeToolbarVisible() {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                getResources().getColor(R.color.colorTransparent),
                getResources().getColor(R.color.colorToolbarTransparent));
        colorAnimation.setDuration(SHORT_ANIMATION_DURATION_IN_MILLIS); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                toolbar.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    @Override
    public int getCurrentCameraId() {
        return currentCameraId;
    }
}
