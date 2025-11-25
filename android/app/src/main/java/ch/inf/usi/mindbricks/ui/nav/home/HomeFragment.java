package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.DialogInterface;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.CoinManager;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startStopButton;
    private ImageView menuIcon;
    private TextView coinBalanceTextView;

    // Timer variables
    private int seconds = 0;
    private boolean isRunning = false;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private CoinManager coinManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        coinManager = new CoinManager(requireActivity().getApplicationContext());

        timerTextView = view.findViewById(R.id.timer_text_view);
        startStopButton = view.findViewById(R.id.start_stop_button);
        menuIcon = view.findViewById(R.id.drawer_menu);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        updateCoinDisplay();

        startStopButton.setOnClickListener(v -> handleStartStop());

        menuIcon.setOnClickListener(v -> {
            DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    /**
     * Handles the logic for the main Start/Stop button.
     */
    private void handleStartStop() {
        if (isRunning) {
            checkEndedSession();
        } else {
            startTimer();
        }
    }

    /**
     * Displays a confirmation dialog before stopping the session.
     */
    public void checkEndedSession() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("End Session?");
        builder.setMessage("Are you sure you want to end the current study session?");

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            stopTimer(); // This also updates the coin display
        });

        builder.setNegativeButton("Abort", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "Session continued", Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Starts the timer and updates the UI.
     */
    private void startTimer() {
        isRunning = true;
        startStopButton.setText(R.string.stop_session);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                updateTimerUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Stops the timer, calculates coins, resets the UI, and updates the coin display.
     */
    private void stopTimer() {
        if (!isRunning) return;

        int minutesStudied = seconds / 60;
        int coinsEarned = minutesStudied;

        if (coinsEarned > 0) {
            coinManager.addCoins(coinsEarned);
            Toast.makeText(getContext(), "You earned " + coinsEarned + " coins!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Session ended. Study for at least a minute to earn coins.", Toast.LENGTH_LONG).show();
        }

        timerHandler.removeCallbacks(timerRunnable);
        seconds = 0;
        isRunning = false;
        startStopButton.setText(R.string.start_session);
        updateTimerUI();

        updateCoinDisplay();
    }

    /**
     * Updates the timer TextView with the properly formatted time.
     */
    private void updateTimerUI() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        timerTextView.setText(timeString);
    }

    /**
     * 5. NEW METHOD TO HANDLE UPDATING THE COIN TEXTVIEW
     * Gets the current balance from CoinManager and sets the text.
     */
    private void updateCoinDisplay() {
        if (coinBalanceTextView != null) {
            int balance = coinManager.getCoinBalance();
            coinBalanceTextView.setText(String.valueOf(balance));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}