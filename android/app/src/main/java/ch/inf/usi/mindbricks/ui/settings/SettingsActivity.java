package ch.inf.usi.mindbricks.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import ch.inf.usi.mindbricks.BuildConfig;
import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarSyncService;
import ch.inf.usi.mindbricks.util.SoundPlayer;

/**
 * Settings activity that displays various settings fragments in a tabbed layout.
 *
 * @author Luca Di Bello
 * @author Marta
 * @author Luca Beltrami
 */
public class SettingsActivity extends AppCompatActivity {

    private CalendarSyncService syncService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);

        SettingsPagerAdapter adapter = new SettingsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.settings_tab_profile);
            } else if (position == 1) {
                tab.setText(R.string.settings_tab_study_plan);
            } else if (position == 2){
                tab.setText(R.string.settings_tab_pomodoro);
            }
            else if (position == 3){
                tab.setText(R.string.settings_tab_calendar);
            }
            // Dev mode - debug tab
            else if (position == 4 && BuildConfig.DEBUG){
                tab.setText(R.string.settings_tab_debug);
            }
        }).attach();

        syncService = CalendarSyncService.getInstance(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            // save_settings sound when leaving the settigs activity
            SoundPlayer.playSound(this, R.raw.save_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Disconnect a calendar
    private void disconnectCalendar(String sourceName) {
        syncService.disconnectDriver(sourceName);
        updateUI();
    }

    // Refresh UI to show connection status
    private void updateUI() {
        List<CalendarSyncService.DriverInfo> drivers = syncService.getDriverInfoList();
        for (CalendarSyncService.DriverInfo info : drivers) {
            Log.d("Calendar", info.displayName +
                    " - Connected: " + info.isConnected +
                    " - Last sync: " + info.getLastSyncTimeString());
        }
    }

    private static class SettingsPagerAdapter extends FragmentStateAdapter {

        public SettingsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new SettingsProfileFragment();
            } else if (position == 1) {
                return new SettingsStudyPlanFragment();
            } else if(position == 2){
                return new SettingsPomodoroFragment();
            }
            else if(position == 3) {
                return new SettingsCalendarFragment();
            }
            else if(position == 4 && BuildConfig.DEBUG){
                return new SettingsDebugFragment();
            }
            // Fallback (should not happen in production)
            return new SettingsProfileFragment();
        }

        @Override
        public int getItemCount() {
            // Show debug tab only in debug builds
            return BuildConfig.DEBUG ? 5 : 4;
        }
    }
}
