package dev.anuran.tod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Correctness checks. Run these before trusting any benchmark number.
 */
public final class Verify {
    private Verify() {}

    public static final int DAY = 86_400;

    public static Map<String, IntUnaryOperator> all() {
        Map<String, IntUnaryOperator> m = new LinkedHashMap<>();
        m.put("traditional1", TimeOfDay::traditional1);
        m.put("traditional2", TimeOfDay::traditional2);
        m.put("traditional3", TimeOfDay::traditional3);
        return m;
    }

    /** Throws if any variant disagrees with the reference anywhere in [0..86399]. */
    public static void assertAllCorrectOverOneDay() {
        for (var e : all().entrySet()) {
            for (int t = 0; t < DAY; t++) {
                int want = TimeOfDay.reference(t);
                int got = e.getValue().applyAsInt(t);
                if (want != got) {
                    throw new IllegalStateException(e.getKey() + " wrong at t=" + t
                            + ": want " + fmt(want) + " got " + fmt(got));
                }
            }
        }
    }

    static String fmt(int p) {
        return String.format("%d:%02d:%02d", TimeOfDay.hour(p), TimeOfDay.minute(p), TimeOfDay.second(p));
    }

    public static void main(String[] args) {
        System.out.println("1. every variant vs reference, t in [0..86399]");
        assertAllCorrectOverOneDay();
        System.out.println("   all " + all().size() + " variants match for all " + DAY + " values");
    }
}
