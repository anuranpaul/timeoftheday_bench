package dev.anuran.tod;

/**
 * Seconds since midnight (0..86399) to hour, minute, second.
 *
 * Every method returns the result packed into one int, so nothing is allocated:
 * (hour << 12) | (minute << 6) | second
 *
 * Based on Ben Joffe's "A faster way to convert a timestamp -> Hour, Min, Sec"
 * (https://www.benjoffe.com/fast-time-of-day). Java has no unsigned ints, so where the
 * article relies on u32 wrap-around I use long and mask.
 */
public final class TimeOfDay {
    private TimeOfDay() {}

    public static int pack(int h, int m, int s) { return (h << 12) | (m << 6) | s; }
    public static int hour(int p)   { return p >>> 12; }
    public static int minute(int p) { return (p >>> 6) & 63; }
    public static int second(int p) { return p & 63; }

    /** Plain version, used to check everything else. */
    public static int reference(int time) {
        return pack(time / 3600, (time / 60) % 60, time % 60);
    }

    /** Sequential: hour, remainder, minute, second. This is what java.time, glibc, Go and CPython do. */
    public static int traditional1(int time) {
        int hour   = time / 3600;
        int rem    = time - hour * 3600;
        int minute = rem / 60;
        int second = rem - minute * 60;
        return pack(hour, minute, second);
    }

    /** Three independent components (V8, Boost, musl). */
    public static int traditional2(int time) {
        int hour   = time / 3600;
        int minute = (time / 60) % 60;
        int second = time % 60;
        return pack(hour, minute, second);
    }

    /** Backwards: minutes first, then split into hour/minute and second. (.NET) */
    public static int traditional3(int time) {
        int tmin   = time / 60;
        int second = time % 60;
        int hour   = tmin / 60;
        int minute = tmin % 60;
        return pack(hour, minute, second);
    }

    // 1193047 = 2^32/3600 + 1, 71582789 = 2^32/60 + 1
    private static final long H_MUL32 = 1193047L;
    private static final long M_MUL32 = 71582789L;

    /** V1: total minutes and hour both come straight from the input, then two subtractions. */
    public static int v1(int time) {
        int tmin   = time / 60;
        int hour   = time / 3600;
        int second = time - tmin * 60;
        int minute = tmin - hour * 60;
        return pack(hour, minute, second);
    }

    /** V1 with the divisions written as multiply-shift. Good for time in [0..2_257_198]. */
    public static int v1Fixed(int time) {
        int tmin   = (int) ((time * M_MUL32) >>> 32);
        int hour   = (int) ((time * H_MUL32) >>> 32);
        int second = time - tmin * 60;
        int minute = tmin - hour * 60;
        return pack(hour, minute, second);
    }
}
