package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout; // Import this
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager; // Import this

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.util.PermissionManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private TextView coinBalanceTextView;
    private ImageView settingsIcon;

    private HomeViewModel homeViewModel;
    private ProfileViewModel profileViewModel;

    private NavigationLocker navigationLocker;

    private List<ImageView> sessionDots;
    private ConstraintLayout sessionDotsLayout; // Add this for the container

    private PermissionManager.PermissionRequest audioPermissionRequest;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // hosting activity implements the NavigationLocker interface
        if (context instanceof NavigationLocker) {
            navigationLocker = (NavigationLocker) context;
        } else {
            throw new RuntimeException(context + " must implement NavigationLocker");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register permission request callback
        audioPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.RECORD_AUDIO,
                this::startDefaultSession,
                () -> Toast.makeText(getContext(), "Microphone permission is required for focus sessions.", Toast.LENGTH_LONG).show()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        // Initialize the shared ViewModel for the user's profile data.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);
        settingsIcon = view.findViewById(R.id.settings_icon);

        // Find the container for the dots
        sessionDotsLayout = view.findViewById(R.id.session_dots_layout);

        // Initialize the list of session dot ImageViews
        sessionDots = new ArrayList<>();
        sessionDots.add(view.findViewById(R.id.dot1));
        sessionDots.add(view.findViewById(R.id.dot2));
        sessionDots.add(view.findViewById(R.id.dot3));
        sessionDots.add(view.findViewById(R.id.dot4));

        // Set a click listener for the settings icon to open the settings dialog
        settingsIcon.setOnClickListener(v -> {
            SettingsFragment settingsDialog = new SettingsFragment();
            settingsDialog.show(getParentFragmentManager(), "SettingsDialog");
        });

        // Set up observers to listen for data changes from the ViewModels
        setupObservers();

        // Notify the ViewModel that the UI is ready
        homeViewModel.activityRecreated();

        startSessionButton.setOnClickListener(v -> {
            // If the timer is running, show a confirmation dialog to stop it
            if (homeViewModel.currentState.getValue() != HomeViewModel.PomodoroState.IDLE) {
                confirmEndSessionDialog();
            } else {
                // Check for permission before starting
                if (PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
                    startDefaultSession();
                } else {
                    audioPermissionRequest.launch();
                }
            }
        });
    }

    // Sets up LiveData observers to automatically update the UI
    private void setupObservers() {
        // Observe the timer's current state
        homeViewModel.currentState.observe(getViewLifecycleOwner(), state -> {
            boolean isRunning = state != HomeViewModel.PomodoroState.IDLE;

            // Update the button text
            startSessionButton.setText(isRunning ? R.string.stop_session : R.string.start_session);

            // Lock the bottom navigation during study session
            navigationLocker.setNavigationEnabled(state != HomeViewModel.PomodoroState.STUDY);

            // Temporarily disable the button to prevent rapid clicks when state changes
            if (isRunning) {
                startSessionButton.setEnabled(false);
                new Handler(Looper.getMainLooper()).postDelayed(() -> startSessionButton.setEnabled(true), 1500);
            } else {
                startSessionButton.setEnabled(true);
            }
            updateSessionDots();
        });

        // Observe events for earning coins
        homeViewModel.earnedCoinsEvent.observe(getViewLifecycleOwner(), amount -> {
            // Check if there is a valid amount of coins to award
            if (amount != null && amount > 0) {
                earnCoin(amount);
                // Reset the event in the ViewModel to prevent it from firing again on config change
                homeViewModel.onCoinsAwarded();
            }
        });

        // Observe the countdown timer's current time to update the display
        homeViewModel.currentTime.observe(getViewLifecycleOwner(), this::updateTimerUI);

        // Observe the user's total coin balance from the shared ProfileViewModel
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });
    }

    // Reads timer durations from SharedPreferences and starts a Pomodoro cycle
    private void startDefaultSession() {
        // Access the saved settings file
        SharedPreferences prefs = requireActivity().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);

        // Retrieve the durations, using default values if none are found
        int studyDuration = (int) prefs.getFloat(SettingsFragment.KEY_STUDY_DURATION, 25.0f);
        int shortPauseDuration = (int) prefs.getFloat(SettingsFragment.KEY_PAUSE_DURATION, 5.0f);
        int longPauseDuration = (int) prefs.getFloat(SettingsFragment.KEY_LONG_PAUSE_DURATION, 15.0f);

        // Tell the ViewModel to start the cycle with these settings
        homeViewModel.pomodoroTechnique(studyDuration, shortPauseDuration, longPauseDuration);
    }


    // Updates the color and width of the session indicator dots based on the current state
    private void updateSessionDots() {
        // Perform a safety check to avoid null pointer exceptions
        if (homeViewModel == null || sessionDots == null || sessionDotsLayout == null) return;

        // Tell TransitionManager to watch the container and animate any layout changes
        TransitionManager.beginDelayedTransition(sessionDotsLayout);

        int currentSession = homeViewModel.getSessionCounter();
        HomeViewModel.PomodoroState currentState = homeViewModel.currentState.getValue();

        //  Reset all dots to their default inactive state
        for (ImageView dot : sessionDots) {
            dot.setImageResource(R.drawable.dot_inactive);

            // Reset layout parameters to be a small 12dp circle
            ViewGroup.LayoutParams params = dot.getLayoutParams();
            params.width = (int) (12 * getResources().getDisplayMetrics().density);
            dot.setLayoutParams(params);
        }

        // If a study session is active, modify the corresponding dot
        if (currentState == HomeViewModel.PomodoroState.STUDY && currentSession > 0 && currentSession <= sessionDots.size()) {
            // Get the currently active dot
            ImageView activeDot = sessionDots.get(currentSession - 1);

            // Change its drawable to the pill shape
            activeDot.setImageResource(R.drawable.dot_active);

            // Change dot width
            ViewGroup.LayoutParams params = activeDot.getLayoutParams();
            params.width = (int) (32 * getResources().getDisplayMetrics().density);
            activeDot.setLayoutParams(params);
        }
    }


    // Shows a confirmation dialog before stopping an active session
    private void confirmEndSessionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Cycle?")
                .setMessage("Are you sure you want to stop the current Pomodoro cycle?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // If confirmed, tell the ViewModel to stop and reset the timer
                    homeViewModel.stopTimerAndReset();
                    Toast.makeText(getContext(), "Pomodoro cycle stopped.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Formats milliseconds into a "MM:SS" string and updates the timer TextView
    private void updateTimerUI(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    // Adds coins to the user's profile and shows a toast message
    private void earnCoin(int amount) {
        if (profileViewModel != null) {
            profileViewModel.addCoins(amount);
        }
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
