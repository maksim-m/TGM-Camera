package me.maxdev.tgmcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import me.maxdev.tgmcamera.util.PermissionsDeniedDialogFragment;

/**
 * Created by Max on 02.05.2016.
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasAllRequiredPermissions()) {
            startCameraActivity();
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        Log.i(LOG_TAG, "CAMERA permission has NOT been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showPermissionsDeniedDialog();
        } else {
            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        request:
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                Log.i(LOG_TAG, "Received response for Camera permission request.");
                if (grantResults.length != REQUIRED_PERMISSIONS.length) {
                    showPermissionsDeniedDialog();
                }
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        showPermissionsDeniedDialog();
                        break request;
                    }
                }
                startCameraActivity();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private boolean hasAllRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    private void showPermissionsDeniedDialog() {
        PermissionsDeniedDialogFragment dialogFragment = new PermissionsDeniedDialogFragment();
        dialogFragment.show(getFragmentManager(), PermissionsDeniedDialogFragment.TAG);
    }
}
