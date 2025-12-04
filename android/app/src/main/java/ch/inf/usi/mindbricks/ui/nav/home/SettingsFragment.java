package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;

public class SettingsFragment extends DialogFragment {

    private TextView studyDurationText, pauseDurationText, longPauseDurationText;
    private Slider studyDurationSlider, pauseDurationSlider, longPauseDurationSlider;
    private Button saveButton;
    private ImageButton closeButton;

    private SharedPreferences sharedPreferences;

    public static final String PREFS_NAME = "TimerSettings";
    public static final String KEY_STUDY_DURATION = "StudyDuration";
    public static final String KEY_PAUSE_DURATION = "PauseDuration";
    public static final String KEY_LONG_PAUSE_DURATION = "LongPauseDuration";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the style to a full-screen dialog theme
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Initialize SharedPreferences when the fragment attaches to the context
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        studyDurationText = view.findViewById(R.id.study_duration_text);
        studyDurationSlider = view.findViewById(R.id.study_duration_slider);
        pauseDurationText = view.findViewById(R.id.pause_duration_text);
        pauseDurationSlider = view.findViewById(R.id.pause_duration_slider);
        longPauseDurationText = view.findViewById(R.id.long_pause_duration_text);
        longPauseDurationSlider = view.findViewById(R.id.long_pause_duration_slider);
        saveButton = view.findViewById(R.id.save_settings_button);
        closeButton = view.findViewById(R.id.close_button);

        // Load previously saved settings into the UI.
        loadSettings();

        // Add a listeners
        studyDurationSlider.addOnChangeListener((slider, value, fromUser) ->
                studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value)));

        pauseDurationSlider.addOnChangeListener((slider, value, fromUser) ->
                pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value)));

        longPauseDurationSlider.addOnChangeListener((slider, value, fromUser) ->
                longPauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value)));

        saveButton.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
            dismiss(); // Close the dialog.
        });

        // Set a click listener for the close button to dismiss the dialog without saving
        closeButton.setOnClickListener(v -> dismiss());
    }

    // Loads the timer values from SharedPreferences and updates the sliders.
    private void loadSettings() {
        // Load study duration, defaulting to 25 minutes
        float studyValue = sharedPreferences.getFloat(KEY_STUDY_DURATION, 25.0f);
        studyDurationSlider.setValue(studyValue);
        studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", studyValue));

        // Load short pause duration, defaulting to 5 minutes
        float pauseValue = sharedPreferences.getFloat(KEY_PAUSE_DURATION, 5.0f);
        pauseDurationSlider.setValue(pauseValue);
        pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", pauseValue));

        // Load long pause duration, defaulting to 15 minutes
        float longPauseValue = sharedPreferences.getFloat(KEY_LONG_PAUSE_DURATION, 15.0f);
        longPauseDurationSlider.setValue(longPauseValue);
        longPauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", longPauseValue));
    }

    // Saves the current slider values to SharedPreferences.
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_STUDY_DURATION, studyDurationSlider.getValue());
        editor.putFloat(KEY_PAUSE_DURATION, pauseDurationSlider.getValue());
        editor.putFloat(KEY_LONG_PAUSE_DURATION, longPauseDurationSlider.getValue());
        editor.apply();
    }
}
