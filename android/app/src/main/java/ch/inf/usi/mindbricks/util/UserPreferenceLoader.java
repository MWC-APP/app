package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton utility that loads and parses user_preferences.json at runtime.
 * Provides null-safe accessor methods for preference values without requiring
 * Java model classes.
 *
 * The JSON file contains user scheduling preferences including:
 * - Sleep schedule (bedtime, wakeup time)
 * - Meal times (breakfast, lunch, dinner)
 * - Work schedule (days, hours)
 * - Study preferences (preferred times, session length)
 * - Exercise schedule
 * - Social time
 * - Calendar integration settings
 * - Personal goals
 *
 * @author Marta Šafářová
 * Refactored by
 * @author Luca Di Bello
 */
public class UserPreferenceLoader {
    private static final String TAG = "UserPreferenceLoader";
    private static final String PREFS_FILE = "user_preferences.json";

    private static UserPreferenceLoader instance;
    private JsonObject preferences;

    private UserPreferenceLoader(Context context) {
        loadPreferences(context);
    }

    /**
     * Get singleton instance of UserPreferenceLoader
     *
     * @param context Application or activity context
     * @return Singleton instance
     */
    public static synchronized UserPreferenceLoader getInstance(Context context) {
        if (instance == null) {
            instance = new UserPreferenceLoader(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Load preferences from assets/user_preferences.json
     */
    private void loadPreferences(Context context) {
        try {
            InputStream is = context.getAssets().open(PREFS_FILE);
            String json = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            preferences = new Gson().fromJson(json, JsonObject.class);
            Log.d(TAG, "Successfully loaded user preferences from " + PREFS_FILE);

        } catch (Exception e) {
            Log.e(TAG, "Failed to load user preferences, using defaults", e);
            preferences = createDefaultPreferences();
        }
    }

    /**
     * Create default preferences if JSON loading fails
     */
    private JsonObject createDefaultPreferences() {
        JsonObject defaults = new JsonObject();

        // Sleep schedule defaults (11pm - 6am, 7.5 hours)
        JsonObject sleepSchedule = new JsonObject();
        JsonObject bedtime = new JsonObject();
        bedtime.addProperty("hour", 23);
        bedtime.addProperty("minute", 0);
        sleepSchedule.add("bedtime", bedtime);

        JsonObject wakeup = new JsonObject();
        wakeup.addProperty("hour", 6);
        wakeup.addProperty("minute", 0);
        sleepSchedule.add("wakeupTime", wakeup);
        sleepSchedule.addProperty("targetSleepHours", 7.5);

        defaults.add("sleepSchedule", sleepSchedule);

        // Meal times defaults
        JsonObject mealTimes = new JsonObject();

        JsonObject breakfast = new JsonObject();
        breakfast.addProperty("hour", 7);
        breakfast.addProperty("minute", 30);
        breakfast.addProperty("duration", 30);
        breakfast.addProperty("enabled", true);
        mealTimes.add("breakfast", breakfast);

        JsonObject lunch = new JsonObject();
        lunch.addProperty("hour", 12);
        lunch.addProperty("minute", 30);
        lunch.addProperty("duration", 45);
        lunch.addProperty("enabled", true);
        mealTimes.add("lunch", lunch);

        JsonObject dinner = new JsonObject();
        dinner.addProperty("hour", 19);
        dinner.addProperty("minute", 0);
        dinner.addProperty("duration", 45);
        dinner.addProperty("enabled", true);
        mealTimes.add("dinner", dinner);

        defaults.add("mealTimes", mealTimes);

        // Work schedule defaults
        JsonObject workSchedule = new JsonObject();
        workSchedule.addProperty("enabled", true);
        JsonArray workDays = new JsonArray();
        workDays.add("MONDAY");
        workDays.add("TUESDAY");
        workDays.add("WEDNESDAY");
        workDays.add("THURSDAY");
        workDays.add("FRIDAY");
        workSchedule.add("workDays", workDays);

        JsonObject startTime = new JsonObject();
        startTime.addProperty("hour", 9);
        startTime.addProperty("minute", 0);
        workSchedule.add("startTime", startTime);

        JsonObject endTime = new JsonObject();
        endTime.addProperty("hour", 17);
        endTime.addProperty("minute", 0);
        workSchedule.add("endTime", endTime);
        workSchedule.addProperty("allowStudyDuringWork", false);

        defaults.add("workSchedule", workSchedule);

        // Calendar integration defaults
        JsonObject calendarIntegration = new JsonObject();
        calendarIntegration.addProperty("enabled", true);
        calendarIntegration.addProperty("bufferBeforeEvent", 15);
        calendarIntegration.addProperty("bufferAfterEvent", 10);
        defaults.add("calendarIntegration", calendarIntegration);

        // Personal goals defaults
        JsonObject personalGoals = new JsonObject();
        personalGoals.addProperty("dailyStudyMinutes", 120);
        personalGoals.addProperty("targetFocusScore", 70);
        defaults.add("personalGoals", personalGoals);

        return defaults;
    }

    // ===== Top-level Accessor Methods =====

    /**
     * Get sleep schedule configuration
     *
     * @return JsonObject with bedtime, wakeupTime, targetSleepHours, etc.
     */
    public JsonObject getSleepSchedule() {
        if (preferences == null || !preferences.has("sleepSchedule")) {
            return null;
        }
        return preferences.getAsJsonObject("sleepSchedule");
    }

    /**
     * Get meal times configuration
     *
     * @return JsonObject with breakfast, lunch, dinner objects
     */
    public JsonObject getMealTimes() {
        if (preferences == null || !preferences.has("mealTimes")) {
            return null;
        }
        return preferences.getAsJsonObject("mealTimes");
    }

    /**
     * Get work schedule configuration
     *
     * @return JsonObject with workDays array, startTime, endTime, etc.
     */
    public JsonObject getWorkSchedule() {
        if (preferences == null || !preferences.has("workSchedule")) {
            return null;
        }
        return preferences.getAsJsonObject("workSchedule");
    }

    /**
     * Get study preferences configuration
     *
     * @return JsonObject with preferredStudyTimes, maxDailyStudyHours, etc.
     */
    public JsonObject getStudyPreferences() {
        if (preferences == null || !preferences.has("studyPreferences")) {
            return null;
        }
        return preferences.getAsJsonObject("studyPreferences");
    }

    /**
     * Get exercise schedule configuration
     *
     * @return JsonObject with enabled flag and preferredTimes array
     */
    public JsonObject getExerciseSchedule() {
        if (preferences == null || !preferences.has("exerciseSchedule")) {
            return null;
        }
        return preferences.getAsJsonObject("exerciseSchedule");
    }

    /**
     * Get social time preferences
     *
     * @return JsonObject with preferredDays and preferredHours arrays
     */
    public JsonObject getSocialTime() {
        if (preferences == null || !preferences.has("socialTime")) {
            return null;
        }
        return preferences.getAsJsonObject("socialTime");
    }

    /**
     * Get calendar integration settings
     *
     * @return JsonObject with enabled flag, buffer times, etc.
     */
    public JsonObject getCalendarIntegration() {
        if (preferences == null || !preferences.has("calendarIntegration")) {
            return null;
        }
        return preferences.getAsJsonObject("calendarIntegration");
    }

    /**
     * Get personal goals configuration
     *
     * @return JsonObject with dailyStudyMinutes, targetFocusScore, etc.
     */
    public JsonObject getPersonalGoals() {
        if (preferences == null || !preferences.has("personalGoals")) {
            return null;
        }
        return preferences.getAsJsonObject("personalGoals");
    }

    // ===== Helper Methods =====

    /**
     * Get hour value from a time object
     *
     * @param timeObject Parent object containing the time field
     * @param key        Key for the time object (e.g., "bedtime", "startTime")
     * @return Hour value (0-23), or 0 if not found
     */
    public int getHour(JsonObject timeObject, String key) {
        if (timeObject == null || !timeObject.has(key)) {
            return 0;
        }

        JsonElement element = timeObject.get(key);
        if (element.isJsonObject()) {
            JsonObject time = element.getAsJsonObject();
            if (time.has("hour")) {
                return time.get("hour").getAsInt();
            }
        }

        return 0;
    }

    /**
     * Get minute value from a time object
     *
     * @param timeObject Parent object containing the time field
     * @param key        Key for the time object
     * @return Minute value (0-59), or 0 if not found
     */
    public int getMinute(JsonObject timeObject, String key) {
        if (timeObject == null || !timeObject.has(key)) {
            return 0;
        }

        JsonElement element = timeObject.get(key);
        if (element.isJsonObject()) {
            JsonObject time = element.getAsJsonObject();
            if (time.has("minute")) {
                return time.get("minute").getAsInt();
            }
        }

        return 0;
    }

    /**
     * Check if a feature is enabled
     *
     * @param object JsonObject to check
     * @return true if "enabled" field is true, false otherwise
     */
    public boolean isEnabled(JsonObject object) {
        if (object == null || !object.has("enabled")) {
            return false;
        }
        return object.get("enabled").getAsBoolean();
    }

    /**
     * Get list of strings from JSON array
     *
     * @param object JsonObject containing the array
     * @param key    Key for the array field
     * @return List of strings, or empty list if not found
     */
    public List<String> getStringList(JsonObject object, String key) {
        List<String> result = new ArrayList<>();

        if (object == null || !object.has(key)) {
            return result;
        }

        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                result.add(item.getAsString());
            }
        }

        return result;
    }

    /**
     * Get list of integers from JSON array
     *
     * @param object JsonObject containing the array
     * @param key    Key for the array field
     * @return List of integers, or empty list if not found
     */
    public List<Integer> getIntList(JsonObject object, String key) {
        List<Integer> result = new ArrayList<>();

        if (object == null || !object.has(key)) {
            return result;
        }

        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                result.add(item.getAsInt());
            }
        }

        return result;
    }

    /**
     * Get integer value from JSON object
     *
     * @param object       JsonObject to read from
     * @param key          Key for the integer field
     * @param defaultValue Default value if not found
     * @return Integer value or default
     */
    public int getInt(JsonObject object, String key, int defaultValue) {
        if (object == null || !object.has(key)) {
            return defaultValue;
        }
        return object.get(key).getAsInt();
    }

    /**
     * Get boolean value from JSON object
     *
     * @param object       JsonObject to read from
     * @param key          Key for the boolean field
     * @param defaultValue Default value if not found
     * @return Boolean value or default
     */
    public boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        if (object == null || !object.has(key)) {
            return defaultValue;
        }
        return object.get(key).getAsBoolean();
    }
}
