package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import ch.inf.usi.mindbricks.R;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startStopButton;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private ImageView menuIcon;

    private int seconds = 0;
    private boolean isRunning = false;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI elements inside the fragment layout
        timerTextView = view.findViewById(R.id.timer_text_view);
        startStopButton = view.findViewById(R.id.start_stop_button);
        drawer = view.findViewById(R.id.drawer_layout);
        menuIcon = view.findViewById(R.id.drawer_menu);            // Top-right icon
        navigationView = view.findViewById(R.id.navigation_view);  // Drawer menu


        // Open the drawer when the menu icon is clicked
        menuIcon.setOnClickListener(v -> drawer.openDrawer(GravityCompat.END));

        // Handle drawer menu item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.drawer_menu) {
                // TODO: open settings fragment/activity here
            }
            drawer.closeDrawer(GravityCompat.END);
            return true;
        });

        // Timer start/stop button
        startStopButton.setOnClickListener(v -> handleStartStop());


        // Setup the drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_settings) {
                // Open Settings fragment or activity
            }
            drawer.closeDrawer(GravityCompat.END);
            return true;
        });

    }

    private void handleStartStop() {
        if (isRunning) {
            // Check confirmation from user before closing the session
            checkEndedSession();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        isRunning = true;
        startStopButton.setText("Stop Session");

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

    private void stopTimer() {
        if (!isRunning) return; // safety check

        // Stop the timer Runnable
        timerHandler.removeCallbacks(timerRunnable);

        // Reset counter
        seconds = 0;
        isRunning = false;

        // Update UI
        startStopButton.setText("Start Session"); // or use string resource
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", 0, 0, 0));
    }


    private void updateTimerUI() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        timerTextView.setText(timeString);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
    public void checkEndedSession(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Are you sure you want to end the session?");
        builder.setMessage("By confirming your house will be demolished!");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Call the method that actually ends the session.
                stopTimer();
            }
        });

        builder.setNegativeButton("Abort", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the Abort button.
                // The dialog will automatically close. You can add a toast if you want.
                dialog.dismiss();
            }
        });

        // To actually create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
