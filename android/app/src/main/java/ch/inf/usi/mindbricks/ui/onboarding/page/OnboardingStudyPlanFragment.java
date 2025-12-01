package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.plan.DayHours;
import ch.inf.usi.mindbricks.model.plan.DayRow;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.Hours;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class OnboardingStudyPlanFragment extends Fragment implements OnboardingStepValidator {

    /**
     * Preferences manager to store/retrieve study plan.
     */
    private PreferencesManager prefs;

    /**
     * List of day rows representing each day of the week.
     */
    private final List<DayRow> dayRows = new ArrayList<>();

    /**
     * TextView displaying the total weekly study hours.
     */
    private MaterialTextView weeklyTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_study_plan, container, false);

        prefs = new PreferencesManager(requireContext());
        weeklyTotal = view.findViewById(R.id.textWeeklyTotal);

        // create and bind day rows
        bindDayRows(view);

        // reload any existing plan from preferences
        restorePlanFromPreferences();

        // recompute weekly total based on initial values
        updateWeeklyTotal();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        persistIfComplete();
    }

    @Override
    public boolean validateStep() {
        List<DayHours> plan = new ArrayList<>();
        boolean hasErrors = false;

        // 1. for each selected day, ensure hours > 0
        for (DayRow row : dayRows) {
            row.checkBox().setError(null);
            // NOTE: skip if day not selected
            if (!row.checkBox().isChecked()) continue;

            float hours = row.slider().getValue();
            if (hours <= 0f) {
                row.checkBox().setError(getString(R.string.onboarding_study_plan_error_hours_required));
                hasErrors = true;
                continue;
            }
            plan.add(new DayHours(row.dayKey(), hours));
        }

        // 2. ensure at least one day is selected
        if (plan.isEmpty()) {
            hasErrors = true;
            View root = getView();
            if (root != null) {
                Snackbar.make(root, R.string.onboarding_study_plan_error_day_required, Snackbar.LENGTH_SHORT).show();
            }
        }

        if (hasErrors) {
            return false;
        }

        prefs.setStudyPlan(plan);
        return true;
    }

    /**
     * Configures the day rows and binds their UI elements with the corresponding events.
     * @param view The root view containing the day rows.
     */
    private void bindDayRows(View view) {
        dayRows.add(bindDayRow(view, R.id.dayMonday, R.string.onboarding_study_plan_day_monday, "monday"));
        dayRows.add(bindDayRow(view, R.id.dayTuesday, R.string.onboarding_study_plan_day_tuesday, "tuesday"));
        dayRows.add(bindDayRow(view, R.id.dayWednesday, R.string.onboarding_study_plan_day_wednesday, "wednesday"));
        dayRows.add(bindDayRow(view, R.id.dayThursday, R.string.onboarding_study_plan_day_thursday, "thursday"));
        dayRows.add(bindDayRow(view, R.id.dayFriday, R.string.onboarding_study_plan_day_friday, "friday"));
        dayRows.add(bindDayRow(view, R.id.daySaturday, R.string.onboarding_study_plan_day_saturday, "saturday"));
        dayRows.add(bindDayRow(view, R.id.daySunday, R.string.onboarding_study_plan_day_sunday, "sunday"));
    }

    /**
     * Binds a single day row's UI elements and sets up their event listeners.
     * @param root The root view containing the day row.
     * @param containerId The ID of the container view for the day row.
     * @param labelRes The string resource ID for the day label.
     * @param dayKey The key representing the day of the week.
     * @return The configured DayRow object.
     */
    private DayRow bindDayRow(View root, int containerId, int labelRes, String dayKey) {
        // get references to UI elements
        View container = root.findViewById(containerId);
        MaterialCheckBox checkBox = container.findViewById(R.id.checkDay);
        Slider slider = container.findViewById(R.id.sliderHours);
        MaterialTextView hoursLabel = container.findViewById(R.id.textDayHours);

        // update UI elements
        checkBox.setText(labelRes);
        updateHoursLabel(hoursLabel, slider.getValue());

        // setup event listeners
        // a) checkbox toggles slider enabled state
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            slider.setEnabled(isChecked);
            if (!isChecked) {
                checkBox.setError(null);
                slider.setValue(0f);
                updateHoursLabel(hoursLabel, 0f);
                // update total hours
                updateWeeklyTotal();
            }
        });
        // b) slider updates hours label
        slider.addOnChangeListener((s, value, fromUser) -> {
            updateHoursLabel(hoursLabel, value);
            // if user sets hours > 0, ensure checkbox is checked
            if (value > 0f && !checkBox.isChecked()) {
                checkBox.setChecked(true);
            }
            if (value > 0f) {
                checkBox.setError(null);
            }
            // update total hours
            updateWeeklyTotal();
        });

        // return configured DayRow object
        return new DayRow(dayKey, checkBox, slider, hoursLabel);
    }

    /**
     * Restores the study plan from preferences and updates the UI accordingly.
     */
    private void restorePlanFromPreferences() {
        List<DayHours> plan = prefs.getStudyPlan();
        for (DayHours dayHours : plan) {
            DayRow row = findRow(dayHours.dayKey());
            if (row != null) {
                row.checkBox().setChecked(true);
                row.slider().setEnabled(true);
                row.slider().setValue(dayHours.hours());
                updateHoursLabel(row.hoursLabel(), dayHours.hours());
            }
        }
        updateWeeklyTotal();
    }

    /**
     * Finds the DayRow corresponding to the given day key.
     * @param dayKey The key representing the day of the week.
     * @return The DayRow object if found, otherwise null.
     */
    private DayRow findRow(String dayKey) {
        for (DayRow row : dayRows) {
            if (row.dayKey().equals(dayKey)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Persists the study plan to preferences if at least one day with non-zero hours is selected.
     */
    private void persistIfComplete() {
        List<DayHours> plan = new ArrayList<>();

        // for each day, check if user has chosen a day and set >0 hours of study on it
        for (DayRow row : dayRows) {
            if (!row.checkBox().isChecked()) continue;
            float hours = row.slider().getValue();
            if (hours > 0f) {
                // only persist days with non-zero hours
                plan.add(new DayHours(row.dayKey(), hours));
            }
        }

        // if there are valid entries, persist the plan in preferences
        if (!plan.isEmpty()) {
            prefs.setStudyPlan(plan);
        }
    }

    /**
     * Updates the hours label for a given day row.
     * @param label The TextView label to update.
     * @param hours The number of hours to display.
     */
    private void updateHoursLabel(MaterialTextView label, float hours) {
        label.setText(getString(R.string.onboarding_study_plan_hours_label) + ": " + Hours.formatHours(hours));
    }


    /**
     * Updates the weekly total hours label based on the selected days and their hours.
     */
    private void updateWeeklyTotal() {
        if (weeklyTotal == null) return;
        float total = 0f;
        for (DayRow row : dayRows) {
            if (row.checkBox().isChecked()) {
                total += row.slider().getValue();
            }
        }
        weeklyTotal.setText(getString(R.string.onboarding_study_plan_total_placeholder, Hours.formatHours(total)));
    }
}
