package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PermissionManager;

public class SessionTimerDialogFragment extends DialogFragment {

    private Slider durationSlider;
    private TextView durationText;
    private Button startTimerButton;
    private HomeViewModel homeViewModel;
    private PermissionManager.PermissionRequest audioPermissionRequest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register permission request callback
        audioPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.RECORD_AUDIO,
                this::startPomodoroSession,
                () -> Toast.makeText(getContext(), "Microphone permission is required for focus sessions.", Toast.LENGTH_LONG).show()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(requireParentFragment(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        return inflater.inflate(R.layout.dialog_timer_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        durationSlider = view.findViewById(R.id.duration_slider);
        durationText = view.findViewById(R.id.duration_text);
        startTimerButton = view.findViewById(R.id.start_stop_button);

        int initialValue = (int) durationSlider.getValue();
        durationText.setText(String.format(Locale.getDefault(), "%d minutes", initialValue));

        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value));
        });

        startTimerButton.setOnClickListener(v -> {
            int studyMinutes = (int) durationSlider.getValue();
            if (studyMinutes > 0) {
                // Define the default pause durations
                int pauseMinutes = 5;
                int longPauseMinutes = 15;


                // Call the ViewModel method with all three required arguments
                homeViewModel.pomodoroTechnique(studyMinutes, pauseMinutes, longPauseMinutes);

                dismiss();
                 // Check for permission before starting
                if (PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
                    startPomodoroSession();
                } else {
                    audioPermissionRequest.launch();
                }
            } else {
                Toast.makeText(getContext(), "Please select a duration greater than 0.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPomodoroSession() {
        int studyMinutes = (int) durationSlider.getValue();
        // FIXME: We need to use the configured long/short pause lengths (Luca is implementing this)
        int pauseMinutes = 5;
        int longPauseMinutes = 15;

        // Call the ViewModel method with all three required arguments
        homeViewModel.pomodoroTechnique(studyMinutes, pauseMinutes, longPauseMinutes);

        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
