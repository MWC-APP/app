package ch.inf.usi.mindbricks.ui.onboarding.page.sensors;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class OnboardingSensorsViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> hasMicrophone = new MutableLiveData<>(false);

    private MutableLiveData<Boolean> hasLuminanceSensor = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasPickUpGestureSensor = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasProximitySensor = new MutableLiveData<>(false);;

    // permissions
    private MutableLiveData<Boolean> hasRecordingPermission = new MutableLiveData<>(false);;


    public OnboardingSensorsViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> getHasMicrophone() {
        return hasMicrophone;
    }

    public MutableLiveData<Boolean> getHasLuminanceSensor() {
        return hasLuminanceSensor;
    }

    public MutableLiveData<Boolean> getHasPickUpGestureSensor() {
        return hasPickUpGestureSensor;
    }

    public MutableLiveData<Boolean> getHasRecordingPermission() {
        return hasRecordingPermission;
    }

    public void setHasRecordingPermission(boolean b) {
        hasRecordingPermission.setValue(b);
    }
}
