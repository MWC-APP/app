package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PermissionManager;

public class OnboardingSensorsFragment extends Fragment {

    private PermissionManager.PermissionRequest micPermissionRequest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Create permission manager for microphone
        micPermissionRequest = PermissionManager.registerSinglePermission(
                this, Manifest.permission.RECORD_AUDIO,
                // on granted callback
                () -> {
                    // update the flag!
                    System.out.println("Microphone access granted");
                },
                // on denied callback
                () -> {
                    System.out.println("Microphone access denied");
                },
                // on rationale callback
                () -> {
                    System.out.println("Microphone access rationale");
                }
        );

        View view = inflater.inflate(R.layout.fragment_onboarding_sensors, container, false);

        MaterialButton micButton = view.findViewById(R.id.buttonEnableMicrophone);
        MaterialButton lightButton = view.findViewById(R.id.buttonEnableLight);

        micButton.setOnClickListener(v -> requestMicrophoneAccess());
        lightButton.setOnClickListener(v -> requestLuminanceAccess());

        return view;
    }

    private void requestMicrophoneAccess() {
        System.out.println("Requesting microphone access");
        micPermissionRequest.checkAndRequest(requireActivity());
    }

    private void requestLuminanceAccess() {
        // intentionally left blank until permissions are wired
    }
}
