package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

public class CoinManager {

    private static final String PREFS_NAME = "MindBricksPrefs";
    private static final String COIN_KEY = "userCoinBalance";
    private final SharedPreferences sharedPreferences;

    public CoinManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the current total coin balance for the user.
     * @return The number of coins the user has.
     */
    public int getCoinBalance() {
        // Returns the saved coin value, or 0 if it has never been set.
        return sharedPreferences.getInt(COIN_KEY, 0);
    }

    /**
     * Adds a specified number of coins to the user's total balance.
     * @param coinsToAdd The number of coins earned in the session.
     */
    public void addCoins(int coinsToAdd) {
        int currentBalance = getCoinBalance();
        int newBalance = currentBalance + coinsToAdd;
        setCoinBalance(newBalance);
    }

    /**
     * Directly sets the user's coin balance.
     * @param newBalance The new total coin balance.
     */
    public void setCoinBalance(int newBalance) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(COIN_KEY, newBalance);
        editor.apply(); // Use apply() for asynchronous saving
    }
}