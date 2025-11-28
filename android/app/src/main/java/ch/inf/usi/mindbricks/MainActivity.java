package ch.inf.usi.mindbricks;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ch.inf.usi.mindbricks.databinding.ActivityMainBinding;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;

public class MainActivity extends AppCompatActivity implements NavigationLocker {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_shop, R.id.navigation_profile)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    /**
     * ADDED: This method is required by the NavigationLocker interface.
     * It enables or disables all the items in the BottomNavigationView menu.
     * @param enabled True to enable navigation, false to disable it.
     */
    @Override
    public void setNavigationEnabled(boolean enabled) {
        // We use the binding object to get a safe reference to the navView.
        if (binding != null && binding.navView != null) {
            for (int i = 0; i < binding.navView.getMenu().size(); i++) {
                binding.navView.getMenu().getItem(i).setEnabled(enabled);
            }
        }
    }
}
