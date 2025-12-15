package ch.inf.usi.mindbricks.util;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ch.inf.usi.mindbricks.BuildConfig;
import ch.inf.usi.mindbricks.config.PreferencesKey;

public class ProfileViewModel extends AndroidViewModel {

    private final PreferencesManager preferencesManager;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

    // _coins is the private mutable LiveData
    private final MutableLiveData<Integer> _coins = new MutableLiveData<>();
    // coins is the public immutable LiveData that UI will observe
    public final LiveData<Integer> coins = _coins;

    public ProfileViewModel(Application application) {
        super(application);
        preferencesManager = new PreferencesManager(application);

        // Load the saved coin balance when the ViewModel is created
        int saved = preferencesManager.getBalance();
        _coins.setValue(saved);

        // Listen for changes in balance in order to update the UI
        // NOTE: this is needed to update UI after adding coins via debug menu
        if (BuildConfig.DEBUG) {
            preferenceListener = (sharedPreferences, key) -> {
                if (PreferencesKey.COIN_BALANCE.getName().equals(key)) {
                    int currentBalance = preferencesManager.getBalance();
                    // publish value
                    if (_coins.getValue() == null || _coins.getValue() != currentBalance) {
                        _coins.postValue(currentBalance);
                    }
                }
            };
            preferencesManager.registerOnSharedPreferenceChangeListener(preferenceListener);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // unregister listener
        if (BuildConfig.DEBUG && preferenceListener != null)
            preferencesManager.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public void addCoins(int amount) {
        int currentBalance = _coins.getValue() != null ? _coins.getValue() : 0;
        int newBalance = currentBalance + amount;
        _coins.setValue(newBalance);
        // Save the new balance to SharedPreferences
        saveCoins(newBalance);
    }

    public boolean spendCoins(int amount) {
        int currentBalance = _coins.getValue() != null ? _coins.getValue() : 0;
        if (currentBalance >= amount) {
            int newBalance = currentBalance - amount;
            _coins.setValue(newBalance);
            saveCoins(newBalance);
            return true; // Purchase successful
        }
        return false; // Not enough coins
    }

    private void saveCoins(int balance) {
        preferencesManager.setBalance(balance);
    }
}
