package ch.inf.usi.mindbricks.ui.onboarding.page.sensors;

import android.Manifest;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PermissionManager;

public class OnboardingSensorsFragment extends Fragment {

    private OnboardingSensorsViewModel viewModel;

    private PermissionManager.PermissionRequest micPermissionRequest;
    private boolean isMicPermissionDirty = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // create view model
        viewModel = new ViewModelProvider(
                requireActivity(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(OnboardingSensorsViewModel.class);

        // Create permission manager for microphone
        micPermissionRequest = PermissionManager.registerSinglePermission(
                this, Manifest.permission.RECORD_AUDIO,
                // on granted callback
                () -> {
                    viewModel.setHasRecordingPermission(true);
                    System.out.println("Microphone access granted");
                },
                // on denied callback
                () -> {
                    viewModel.setHasRecordingPermission(false);
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

        // setup on click listeners
        micButton.setOnClickListener(v -> requestMicrophoneAccess());
        lightButton.setOnClickListener(v -> requestLuminanceAccess());

        // setup listeners for sensor availability / permissions
        viewModel.getHasRecordingPermission().observe(getViewLifecycleOwner(), hasPermission -> {
            // NOTE: if the user doesn't have permissions when the activity loads, show the default button
            if (!hasPermission && !isMicPermissionDirty) return;

            if (hasPermission) {
                // FIXME: these colors must be changed actually
                int bg = resolveAttrColor(com.google.android.material.R.attr.colorTertiaryContainer);
                int fg = resolveAttrColor(com.google.android.material.R.attr.colorOnTertiaryContainer);

                // FIXME: add icon to button
                micButton.setBackgroundTintList(ColorStateList.valueOf(bg));
                micButton.setIconTint(ColorStateList.valueOf(fg));
            } else {
                int disabledColor = resolveAttrColor(com.google.android.material.R.attr.colorErrorContainer);
                micButton.setBackgroundTintList(ColorStateList.valueOf(disabledColor));
            }
        });

        return view;
    }

    private int resolveAttrColor(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void requestMicrophoneAccess() {
        System.out.println("Requesting microphone access");
        isMicPermissionDirty = true;
        micPermissionRequest.checkAndRequest(requireActivity());
    }

    private void requestLuminanceAccess() {
        // intentionally left blank until permissions are wired
    }
}
