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
        m.put("v1", TimeOfDay::v1);
        m.put("v1Fixed", TimeOfDay::v1Fixed);
        m.put("v2", TimeOfDay::v2);
        m.put("v3", TimeOfDay::v3);
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

    // Beyond a day the hour can pass 31, so compare against the same packing instead of unpacking.
    private static boolean matchesReference(int t, int packed) {
        return TimeOfDay.pack(t / 3600, (t / 60) % 60, t % 60) == packed;
    }

    private static void checkRange(String name, IntUnaryOperator f, int max) {
        for (int t = 0; t <= max; t++) {
            if (!matchesReference(t, f.applyAsInt(t))) {
                System.out.printf("%-8s FAILS inside the claimed range at t=%d%n", name, t);
                return;
            }
        }
        System.out.printf("%-8s ok for every t in [0..%,d]%n", name, max);
    }

    private static void firstFailureAfter(String name, IntUnaryOperator f, int claimedMax) {
        for (int t = claimedMax + 1; t <= 5_000_000; t++) {
            if (!matchesReference(t, f.applyAsInt(t))) {
                System.out.printf("%-8s first wrong value: t=%,d (claimed max %,d)%n", name, t, claimedMax);
                return;
            }
        }
        System.out.printf("%-8s no failure up to 5,000,000 (claimed max %,d)%n", name, claimedMax);
    }

    public static void main(String[] args) {
        System.out.println("1. every variant vs reference, t in [0..86399]");
        assertAllCorrectOverOneDay();
        System.out.println("   all " + all().size() + " variants match for all " + DAY + " values");

        System.out.println("\n2. claimed input ranges (exhaustive up to the claimed max)");
        checkRange("v1Fixed", TimeOfDay::v1Fixed, 2257198);
        checkRange("v3", TimeOfDay::v3, 2257198);
        checkRange("v2", TimeOfDay::v2, 2255818);

        System.out.println("\n3. first failure just past each range");
        firstFailureAfter("v1Fixed", TimeOfDay::v1Fixed, 2257198);
        firstFailureAfter("v3", TimeOfDay::v3, 2257198);
        firstFailureAfter("v2", TimeOfDay::v2, 2255818);
    }
}
