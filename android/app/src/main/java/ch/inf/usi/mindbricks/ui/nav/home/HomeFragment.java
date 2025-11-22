package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    }

    private void handleStartStop() {
        if (isRunning) {
            stopTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        isRunning = true;
        startStopButton.setText("Stop");

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
        isRunning = false;
        startStopButton.setText("Start");
        timerHandler.removeCallbacks(timerRunnable);
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
}
