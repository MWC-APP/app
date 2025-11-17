package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import ch.inf.usi.mindbricks.R;

public class OnboardingSensorsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sensors, container, false);

        MaterialButton micButton = view.findViewById(R.id.buttonEnableMicrophone);
        MaterialButton lightButton = view.findViewById(R.id.buttonEnableLight);

        micButton.setOnClickListener(v -> requestMicrophoneAccess());
        lightButton.setOnClickListener(v -> requestLuminanceAccess());

        return view;
    }

    private void requestMicrophoneAccess() {
        // intentionally left blank until permissions are wired
    }

    private void requestLuminanceAccess() {
        // intentionally left blank until permissions are wired
    }
}
