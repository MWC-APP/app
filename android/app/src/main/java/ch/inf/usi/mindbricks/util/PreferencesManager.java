package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.inf.usi.mindbricks.config.PreferencesKey;
import ch.inf.usi.mindbricks.game.TileWorldState;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.plan.DayHours;
import ch.inf.usi.mindbricks.model.plan.DayKey;

public class PreferencesManager {

    private static final String PREFS_NAME = "MindBricks-Preferences";

    private final SharedPreferences preferences;
    private final Gson gson;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    // -- Onboarding flag --
    public void setOnboardingComplete() {
        preferences.edit().putBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), true).apply();
    }

    public boolean isOnboardingComplete() {
        return preferences.getBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), false);
    }


    public String getUserName() {
        return preferences.getString(PreferencesKey.USER_NAME.getName(), "");
    }

    // -- User name --
    public void setUserName(String name) {
        preferences.edit().putString(PreferencesKey.USER_NAME.getName(), name).apply();
    }

    public List<Tag> getUserTags() {
        String json = preferences.getString(PreferencesKey.USER_TAGS_JSON.getName(), "[]");
        Type type = new TypeToken<List<Tag>>() {}.getType();
        List<Tag> tags = gson.fromJson(json, type);
        return tags != null ? tags : new ArrayList<>();
    }

    // -- User tags --
    public void setUserTags(List<Tag> tags) {
        preferences.edit().putString(PreferencesKey.USER_TAGS_JSON.getName(), gson.toJson(tags)).apply();
    }

    public String getUserAvatarSeed() {
        return preferences.getString(PreferencesKey.USER_AVATAR_SEED.getName(), "");
    }

    // -- User avatar seed --
    public void setUserAvatarSeed(String seed) {
        preferences.edit().putString(PreferencesKey.USER_AVATAR_SEED.getName(), seed).apply();
    }

    // -- User avatar URI --
    public void setUserAvatarUri(String uri) {
        preferences.edit().putString(PreferencesKey.USER_AVATAR_URI.getName(), uri).apply();
    }

    public String getUserAvatarUri() {
        return preferences.getString(PreferencesKey.USER_AVATAR_URI.getName(), null);
    }

    public Set<String> getPurchasedItemIds() {
        // Retrieve the stored set. The second argument is the default value if the key is not found
        Set<String> storedSet = preferences.getStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), new HashSet<>());
        // Return a new HashSet to prevent modification of the set stored in SharedPreferences.
        return new HashSet<>(storedSet);
    }

    // -- Study plan --
    public void setStudyObjective(String objective) {
        preferences.edit().putString(PreferencesKey.STUDY_OBJECTIVE.getName(), objective).apply();
    }
    public String getStudyObjective() {
        return preferences.getString(PreferencesKey.STUDY_OBJECTIVE.getName(), "");
    }

    public boolean isStudyGoalSet() {
        return preferences.getBoolean(PreferencesKey.STUDY_GOAL_SET.getName(), false);
    }

    public void setStudyGoalSet(boolean isSet) {
        preferences.edit().putBoolean(PreferencesKey.STUDY_GOAL_SET.getName(), isSet).apply();
    }

    public void setStudyPlan(List<DayHours> plan) {
        preferences.edit().putString(PreferencesKey.STUDY_PLAN_JSON.getName(), gson.toJson(plan)).apply();
    }

    public List<DayHours> getStudyPlan() {
        String json = preferences.getString(PreferencesKey.STUDY_PLAN_JSON.getName(), "[]");
        Type type = new TypeToken<List<DayHours>>() {}.getType();
        List<DayHours> plan = gson.fromJson(json, type);
        return plan != null ? plan : new ArrayList<>();
    }

    // -- Timer Settings --
    public int getTimerStudyDuration() {
        return preferences.getInt(PreferencesKey.TIMER_STUDY_DURATION.getName(), 25);
    }

    public void setTimerStudyDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_STUDY_DURATION.getName(), minutes).apply();
    }

    public int getTimerShortPauseDuration() {
        return preferences.getInt(PreferencesKey.TIMER_SHORT_PAUSE_DURATION.getName(), 5);
    }

    public void setTimerShortPauseDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_SHORT_PAUSE_DURATION.getName(), minutes).apply();
    }

    public int getTimerLongPauseDuration() {
        return preferences.getInt(PreferencesKey.TIMER_LONG_PAUSE_DURATION.getName(), 15);
    }

    public void setTimerLongPauseDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_LONG_PAUSE_DURATION.getName(), minutes).apply();
    }

    public boolean isFirstSession() {
        return preferences.getBoolean(PreferencesKey.IS_FIRST_SESSION.getName(), true);
    }

    public void setFirstSession(boolean isFirst) {
        preferences.edit().putBoolean(PreferencesKey.IS_FIRST_SESSION.getName(), isFirst).apply();
    }

    // -- Coin Balance --
    public int getBalance() {
        return preferences.getInt(PreferencesKey.COIN_BALANCE.getName(), 0);
    }

    public void setBalance(int balance) {
        preferences.edit().putInt(PreferencesKey.COIN_BALANCE.getName(), balance).apply();
    }

    // -- Inventory manager --
    public void saveInventory(Map<String, Integer> inventory) {
        preferences.edit().putString(PreferencesKey.INVENTORY_JSON.getName(),
                gson.toJson(inventory)).apply();
    }

    @NonNull
    public Map<String, Integer> getInventory() {
        // get raw json
        String json = preferences.getString(PreferencesKey.INVENTORY_JSON.getName(), null);
        if (json == null || json.isEmpty()) return new HashMap<>();

        // convert to map
        Type mapType = new TypeToken<Map<String, Integer>>() {
        }.getType();
        Map<String, Integer> parsed = gson.fromJson(json, mapType);

        // return parsed map or empty map if null
        return parsed != null ? new HashMap<>(parsed) : new HashMap<>();
    }

    // -- World state --
    public void saveWorldState(TileWorldState state) {
        preferences.edit().putString(PreferencesKey.WORLD_STATE_JSON.getName(),
                gson.toJson(state)).apply();
    }

    @Nullable
    public TileWorldState getWorldState() {
        String json = preferences.getString(PreferencesKey.WORLD_STATE_JSON.getName(), null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return gson.fromJson(json, TileWorldState.class);
    }

    // helper methods
    public int getDailyStudyMinutesGoal(long milliseconds) {
       // get study plan
        List<DayHours> plan = getStudyPlan();

        // extract day of the week
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        // get key for this precise day
        DayKey dayKey = DayKey.fromIndex(dayOfWeek);

        // get study time for the day
        for (DayHours day : plan) {
            if (day.dayKey() == dayKey) {
                return (int) Math.floor(day.hours() * 60);
            }
        }

        // NOTE: we don't have an objective, so we return 0 minutes
        return 0;
    }
}
