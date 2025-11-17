package ch.inf.usi.mindbricks.util.permission;

import androidx.activity.result.ActivityResultCaller;

public interface OnPermissionChangeListener extends ActivityResultCaller {
    void onPermissionGranted(String permission);
    void onPermissionDenied(String permission);
    void onPermissionRationaleNeeded(String permission);
}
