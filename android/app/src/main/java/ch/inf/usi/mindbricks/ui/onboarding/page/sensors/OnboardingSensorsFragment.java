package ch.inf.usi.mindbricks.ui.onboarding.page.sensors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.drivers.LightSensor;
import ch.inf.usi.mindbricks.drivers.SignificantMotionSensor;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;

public class OnboardingSensorsFragment extends Fragment implements OnboardingStepValidator {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_onboarding_sensors, container, false);

        // initialize sensors to check availability
        LightSensor.initialize(requireContext());
        SignificantMotionSensor.initialize(requireContext());

        // get view references
        MaterialButton micInfoButton = rootView.findViewById(R.id.buttonMicBackgroundInfo);
        MaterialButton lightInfoButton = rootView.findViewById(R.id.buttonLightInfo);
        MaterialButton pickupInfoButton = rootView.findViewById(R.id.buttonEnablePickup);
        MaterialTextView lightStatusText = rootView.findViewById(R.id.textLightStatus);
        MaterialTextView pickupStatusText = rootView.findViewById(R.id.textPickupStatus);

        // setup info button click listeners
        micInfoButton.setOnClickListener(v -> showMicrophoneInfo());
        lightInfoButton.setOnClickListener(v -> showLightSensorInfo());
        pickupInfoButton.setOnClickListener(v -> showMotionSensorInfo());

        // check sensor availability and update status
        updateSensorStatus(lightStatusText, pickupStatusText);

        return rootView;
    }

    /**
     * Updates the status text for sensors based on availability
     */
    private void updateSensorStatus(MaterialTextView lightStatus, MaterialTextView pickupStatus) {
        // check light sensor
        if (!LightSensor.getInstance().isAvailable()) {
            lightStatus.setVisibility(View.VISIBLE);
            lightStatus.setText(R.string.onboarding_sensors_light_unavailable);
        } else {
            lightStatus.setVisibility(View.GONE);
        }

        // check motion sensor
        if (!SignificantMotionSensor.getInstance().isAvailable()) {
            pickupStatus.setVisibility(View.VISIBLE);
            pickupStatus.setText(R.string.onboarding_sensors_pickup_unavailable);
        } else if (SignificantMotionSensor.getInstance().isFallback()) {
            pickupStatus.setVisibility(View.VISIBLE);
            pickupStatus.setText(R.string.onboarding_sensors_pickup_fallback_in_use);
        } else {
            pickupStatus.setVisibility(View.GONE);
        }
    }

    /**
     * No validation needed (this onboarding step it's only informative)
     */
    @Override
    public boolean validateStep() {
        return true;
    }

    /**
     * Shows information about microphone usage
     */
    private void showMicrophoneInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_microphone_rationale_title)
                .setMessage(R.string.onboarding_sensors_microphone_rationale)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Shows information about light sensor usage
     */
    private void showLightSensorInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_light_title)
                .setMessage(R.string.onboarding_sensors_light_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Shows information about motion sensor usage
     */
    private void showMotionSensorInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_pickup_info_title)
                .setMessage(R.string.onboarding_sensors_pickup_info_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
