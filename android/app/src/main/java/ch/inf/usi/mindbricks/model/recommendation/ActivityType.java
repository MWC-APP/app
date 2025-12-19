package ch.inf.usi.mindbricks.model.recommendation;

import android.content.Context;

import ch.inf.usi.mindbricks.R;

public enum ActivityType {
    DEEP_STUDY("Deep Study", R.color.activity_deep_study),
    LIGHT_STUDY("Light Study", R.color.activity_light_study),
    WORK("Work/Tasks", R.color.activity_work),
    EXERCISE("Exercise", R.color.activity_exercise),
    SOCIAL("Social Time", R.color.activity_social),
    MEALS("Meals", R.color.activity_meals),
    BREAKS("Short Breaks", R.color.activity_breaks),
    SLEEP("Sleep", R.color.activity_sleep),
    CALENDAR_EVENT("Calendar Event", R.color.activity_calendar);

    private final String displayName;
    private final int colorResId;

    ActivityType(String displayName, int colorResId) {
        this.displayName = displayName;
        this.colorResId = colorResId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHex(Context ctx) {
        int color = ctx.getColor(colorResId);
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public boolean isProtected() {
        return this == CALENDAR_EVENT || this == SLEEP;
    }
}
