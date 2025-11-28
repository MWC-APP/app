package ch.inf.usi.mindbricks;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ch.inf.usi.mindbricks.databinding.ActivityMainBinding;
import ch.inf.usi.mindbricks.ui.analytics.AnalyticsActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_shop, R.id.navigation_profile)
                .build();

        // Setup navigation controller
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Handle bottom navigation item selection
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_analytics) {
                // Launch Analytics Activity
                Intent intent = new Intent(this, AnalyticsActivity.class);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.navigation_home) {
                // Navigate to home fragment
                navController.navigate(R.id.navigation_home);
                return true;
            } else if (itemId == R.id.navigation_shop) {
                // Navigate to shop fragment
                navController.navigate(R.id.navigation_shop);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Navigate to profile fragment
                navController.navigate(R.id.navigation_profile);
                return true;
            }

            return false;
        });
    }
}