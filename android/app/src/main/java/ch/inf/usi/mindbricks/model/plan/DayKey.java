package ch.inf.usi.mindbricks.model.plan;

/**
 * Enum representing the days of the week.
 *
 * @author Luca Di Bello
 */
public enum DayKey {
    MONDAY("monday", (short) 0),
    TUESDAY("tuesday", (short) 1),
    WEDNESDAY("wednesday", (short) 2),
    THURSDAY("thursday", (short) 3),
    FRIDAY("friday", (short) 4),
    SATURDAY("saturday", (short) 5),
    SUNDAY("sunday", (short) 6);

    /**
     * Unique key for each day.
     */
    private final String key;

    /**
     * Index of the day in the week where Monday is 0, and Sunday is 6.
     */
    private final short index;

    /**
     * Constructor for DayKey.
     *
     * @param key Unique key for each day
     * @param index Index of the day in the week where Monday is 0, and Sunday is 6
     */
    DayKey(String key, short index) {
        this.key = key;
        this.index = index;
    }

    /**
     * Get the unique key for the day.
     *
     * @return Unique key for the day
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the index of the day in the week.
     *
     * @return Index of the day in the week
     */
    public short getIndex() {
        return index;
    }

    public static DayKey fromIndex(int dayOfWeek) {
        return switch(dayOfWeek) {
            case 0 -> MONDAY;
            case 1 -> TUESDAY;
            case 2 -> WEDNESDAY;
            case 3 -> THURSDAY;
            case 4 -> FRIDAY;
            case 5 -> SATURDAY;
            case 6 -> SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        };
    }
}
