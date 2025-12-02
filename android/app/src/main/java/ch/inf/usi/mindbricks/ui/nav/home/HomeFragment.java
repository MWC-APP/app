package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.util.PermissionManager;
import ch.inf.usi.mindbricks.util.PermissionManager.PermissionRequest;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    // --- UI Elements ---
    private TextView timerTextView;
    private Button startSessionButton;
    private TextView coinBalanceTextView;

    // --- ViewModels ---
    private HomeViewModel homeViewModel;
    private ProfileViewModel profileViewModel;

    // --- Utilities ---
    private PermissionRequest micPermissionRequest;
    private Integer pendingDurationMinutes = null;
    private NavigationLocker navigationLocker;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationLocker) {
            navigationLocker = (NavigationLocker) context;
        } else {
            throw new RuntimeException(context + " must implement NavigationLocker");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Correctly initialize the AndroidViewModel using its factory
        homeViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        // Setup permission handling and LiveData observers
        setupPermissionManager();
        setupObservers();

        // Tell the ViewModel the view is ready, in case it needs to resume after a screen rotation
        homeViewModel.activityRecreated();

        // Set the main button's click listener
        startSessionButton.setOnClickListener(v -> {
            // The fragment doesn't know the state, it asks the ViewModel
            if (Boolean.TRUE.equals(homeViewModel.isTimerRunning.getValue())) {
                confirmEndSessionDialog();
            } else {
                showDurationPickerDialog();
            }
        });
    }

    private void setupPermissionManager() {
        micPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.RECORD_AUDIO,
                () -> {
                    if (pendingDurationMinutes != null) {
                        startTimerWithPermissionCheck(pendingDurationMinutes);
                        pendingDurationMinutes = null;
                    }
                },
                () -> {
                    pendingDurationMinutes = null;
                    Toast.makeText(getContext(), "Microphone permission is required to record ambient noise.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Sets up all LiveData observers to connect the ViewModel to the UI.
     */
    private void setupObservers() {
        // Observes the timer's running state to control UI
        homeViewModel.isTimerRunning.observe(getViewLifecycleOwner(), isRunning -> {
            navigationLocker.setNavigationEnabled(!isRunning);
            startSessionButton.setText(isRunning ? R.string.stop_session : R.string.start_session);

            // Temporarily disable the button to prevent rapid clicks, then re-enable
            startSessionButton.setEnabled(false);
            new Handler(Looper.getMainLooper()).postDelayed(() -> startSessionButton.setEnabled(true), 1500);

            if (!isRunning) {
                // When timer stops, reset the text to 00:00
                updateTimerUI(0);
            }
        });

        // Observes the countdown ticks and updates the timer text
        homeViewModel.currentTime.observe(getViewLifecycleOwner(), this::updateTimerUI);

        // Observes coin earning events
        homeViewModel.earnedCoinsEvent.observe(getViewLifecycleOwner(), amount -> {
            if (amount != null && amount > 0) {
                earnCoin(amount);
                homeViewModel.onCoinsAwarded(); // Tell ViewModel event was handled
            }
        });

        // Observes the session completion event
        homeViewModel.sessionCompleteEvent.observe(getViewLifecycleOwner(), aVoid -> {
            showSessionCompleteDialog();
            homeViewModel.onSessionCompleted(); // Tell ViewModel event was handled
        });

        // Observes coin balance from the ProfileViewModel
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });
    }

    private void showDurationPickerDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_timer_session, null);

        final Slider durationSlider = dialogView.findViewById(R.id.duration_slider);
        final TextView durationText = dialogView.findViewById(R.id.duration_text);

        durationText.setText(String.format(Locale.getDefault(), "%d minutes", 25));
        durationSlider.addOnChangeListener((slider, value, fromUser) ->
                durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value)));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Set Session Duration")
                .setPositiveButton("Start", (dialog, which) -> {
                    int durationInMinutes = (int) durationSlider.getValue();
                    if (durationInMinutes > 0) {
                        startTimerWithPermissionCheck(durationInMinutes);
                    } else {
                        Toast.makeText(getContext(), "Please select a duration greater than 0.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("MissingPermission")
    private void startTimerWithPermissionCheck(int minutes) {
        if (!PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            pendingDurationMinutes = minutes;
            micPermissionRequest.launch();
            return;
        }
        // Tell the ViewModel to start the timer
        homeViewModel.startTimer(minutes);
    }

    private void confirmEndSessionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Session?")
                .setMessage("Are you sure you want to end the current session early? You will not get a coin for the current minute.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    homeViewModel.stopTimerAndReset(); // Tell ViewModel to stop
                    Toast.makeText(getContext(), "Session ended.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSessionCompleteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Session Complete!")
                .setMessage("Great focus! You've earned 3 bonus coins for completing the session.")
                .setPositiveButton("Awesome!", (dialog, which) -> earnCoin(3))
                .setCancelable(false)
                .show();
    }

    private void updateTimerUI(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void earnCoin(int amount) {
        if (profileViewModel != null) {
            profileViewModel.addCoins(amount);
        }
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
