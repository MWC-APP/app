package ch.inf.usi.mindbricks.model.visual;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class DateRange {
    public enum RangeType {
        LAST_N_DAYS,
        SPECIFIC_MONTH,
        CUSTOM,
        ALL_TIME
    }

    // Core range fields - immutable
    private final long startTimestamp;
    private final long endTimestamp;
    private final RangeType rangeType;

    // Type-specific metadata - immutable and nullable
    private final Integer month;      // For SPECIFIC_MONTH: 0-11 (Calendar.MONTH)
    private final Integer year;
    private final Integer daysCount;  // For LAST_N_DAYS: 1, 7, 30...


    public DateRange() {
        this(0L, Long.MAX_VALUE, RangeType.CUSTOM, null, null, null);
    }

    private DateRange(long startTimestamp, long endTimestamp, RangeType rangeType,
                      Integer month, Integer year, Integer daysCount) {
        // Validation
        if (startTimestamp > endTimestamp) {
            throw new IllegalArgumentException("Start timestamp cannot be after end timestamp");
        }
        if (rangeType == null) {
            throw new IllegalArgumentException("RangeType cannot be null");
        }

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.rangeType = rangeType;
        this.month = month;
        this.year = year;
        this.daysCount = daysCount;
    }

    // methods

    public static DateRange lastNDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive, got: " + days);
        }

        Calendar cal = Calendar.getInstance();
        long endTime = cal.getTimeInMillis(); // Now

        // Go back N days and set to start of that day
        cal.add(Calendar.DAY_OF_MONTH, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        return new DateRange(startTime, endTime, RangeType.LAST_N_DAYS,
                null, null, days);
    }

    public static DateRange forMonth(int year, int month) {
        // Validate inputs
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year out of reasonable range: " + year);
        }
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Month must be 0-11 (Calendar.JANUARY to DECEMBER), got: " + month);
        }

        Calendar cal = Calendar.getInstance();

        // Start of month: 1st day at 00:00:00.000
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        // End of month: last day at 23:59:59.999
        // getActualMaximum handles different month lengths (28-31 days)
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();

        return new DateRange(startTime, endTime, RangeType.SPECIFIC_MONTH,
                month, year, null);
    }

    public static DateRange custom(long startTimestamp, long endTimestamp) {
        return new DateRange(startTimestamp, endTimestamp, RangeType.CUSTOM,
                null, null, null);
    }

    public static DateRange allTime() {
        //Start is set to epoch (1970) and end is set far in the future.
        long start = 0L;
        long end = Long.MAX_VALUE;
        return new DateRange(start, end, RangeType.ALL_TIME, null, null, null);
    }

    public static DateRange currentMonth() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        return forMonth(year, month);
    }

    // helpers
    public boolean contains(long timestamp) {
        return timestamp >= startTimestamp && timestamp <= endTimestamp;
    }

    public boolean containsSession(StudySession session) {
        return session != null && contains(session.getTimestamp());
    }

    public String getDisplayName() {
        switch (rangeType) {
            case LAST_N_DAYS:
                return "Last " + daysCount + (daysCount == 1 ? " Day" : " Days");

            case SPECIFIC_MONTH:
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, 1);
                return monthYearFormat.format(cal.getTime());

            case CUSTOM:
                SimpleDateFormat customFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String start = customFormat.format(new Date(startTimestamp));
                String end = customFormat.format(new Date(endTimestamp));
                return start + " - " + end;

            case ALL_TIME:
                return "All Time";

            default:
                return "Unknown Range";
        }
    }

    public String getShortDisplayName() {
        switch (rangeType) {
            case LAST_N_DAYS:
                return daysCount + "D";

            case SPECIFIC_MONTH:
                SimpleDateFormat shortFormat = new SimpleDateFormat("MMM ''yy", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, 1);
                return shortFormat.format(cal.getTime());

            case CUSTOM:
                return "Custom";

            case ALL_TIME:
                return "All";

            default:
                return "?";
        }
    }

    public int getDurationInDays() {
        long durationMs = endTimestamp - startTimestamp;
        long durationDays = durationMs / (24 * 60 * 60 * 1000);
        return (int) Math.max(1, durationDays); // At least 1 day
    }

    public DateRange previousMonth() {
        if (rangeType != RangeType.SPECIFIC_MONTH) {
            throw new UnsupportedOperationException("previousMonth() only works for SPECIFIC_MONTH ranges");
        }

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        cal.add(Calendar.MONTH, -1);

        int prevYear = cal.get(Calendar.YEAR);
        int prevMonth = cal.get(Calendar.MONTH);

        return forMonth(prevYear, prevMonth);
    }

    public DateRange nextMonth() {
        if (rangeType != RangeType.SPECIFIC_MONTH) {
            throw new UnsupportedOperationException("nextMonth() only works for SPECIFIC_MONTH ranges");
        }

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        cal.add(Calendar.MONTH, 1);

        int nextYear = cal.get(Calendar.YEAR);
        int nextMonth = cal.get(Calendar.MONTH);

        return forMonth(nextYear, nextMonth);
    }

    public boolean isInFuture() {
        return startTimestamp > System.currentTimeMillis();
    }

    // getters

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public RangeType getRangeType() {
        return rangeType;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getDaysCount() {
        return daysCount;
    }

    // object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateRange dateRange = (DateRange) o;

        return startTimestamp == dateRange.startTimestamp &&
                endTimestamp == dateRange.endTimestamp &&
                rangeType == dateRange.rangeType &&
                Objects.equals(month, dateRange.month) &&
                Objects.equals(year, dateRange.year) &&
                Objects.equals(daysCount, dateRange.daysCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTimestamp, endTimestamp, rangeType, month, year, daysCount);
    }

    @Override
    public String toString() {
        return "DateRange{" +
                "type=" + rangeType +
                ", display='" + getDisplayName() + '\'' +
                ", start=" + startTimestamp +
                ", end=" + endTimestamp +
                '}';
    }
}