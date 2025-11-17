package ch.inf.usi.mindbricks.util.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class PermissionManager {

    private ActivityResultLauncher<String> launcher;
    private final String permission;
    // listener for changes
    private OnPermissionChangeListener listener;
    public PermissionManager(final String permission, OnPermissionChangeListener listener) {
        this.listener = listener;
        this.permission = permission;

        // create permission launcher
        launcher = listener.registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                listener.onPermissionGranted(permission);
            } else {
                listener.onPermissionDenied(permission);
            }
        });
    }

    public void setup(Activity activity) {
        if (hasPermission(permission, activity)) {
            listener.onPermissionGranted(permission);
        } else if (shouldRequestRationale(permission, activity)) {
            listener.onPermissionRationaleNeeded(permission);
        } else {
            // request permission from user
            launcher.launch(permission);
        }
    }

    public boolean hasPermission(String permission, Context context) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean shouldRequestRationale(String permission, Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
}
