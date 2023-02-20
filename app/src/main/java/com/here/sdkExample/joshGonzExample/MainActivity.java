package com.here.sdkExample.joshGonzExample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.here.android.mpa.common.PositioningManager;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // positioning manager instance
    private PositioningManager mPositioningManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            setupMapFragmentView();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPositioningManager != null) {
            mPositioningManager.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);
        }
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int index = 0; index < permissions.length; index++) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                    /*
                     * If the user turned down the permission request in the past and chose the
                     * Don't ask again option in the permission request system dialog.
                     */
                    if (!ActivityCompat
                            .shouldShowRequestPermissionRationale(this, permissions[index])) {
                        Toast.makeText(this, "Required permission " + permissions[index]
                                        + " not granted. "
                                        + "Please go to settings and turn on for sample app",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Required permission " + permissions[index]
                                + " not granted", Toast.LENGTH_LONG).show();
                    }
                }
            }

            setupMapFragmentView();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setupMapFragmentView() {
        // All permission requests are being handled. Create map fragment view. Please note
        // the HERE Mobile SDK requires all permissions defined above to operate properly.
        new MapFragmentView(this);
        invalidateOptionsMenu();
    }
}