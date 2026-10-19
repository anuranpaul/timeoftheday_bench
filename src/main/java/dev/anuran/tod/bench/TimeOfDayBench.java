package dev.anuran.tod.bench;

import dev.anuran.tod.TimeOfDay;
import dev.anuran.tod.Verify;
import org.openjdk.jmh.annotations.*;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * tp_*  : independent conversions in a loop (throughput)
 * lat_* : each result feeds the next input, so nothing can overlap (latency)
 * *_baseline : same loop with the conversion removed; summarize.py subtracts it
 * *_javaTime : LocalTime.ofSecondOfDay for reference. It allocates, so it's not a fair race.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class TimeOfDayBench {

    static final int N = 65_536;

    int[] times;

    @Setup(Level.Trial)
    public void setup() {
        Verify.assertAllCorrectOverOneDay();   // don't benchmark wrong code
        Random rnd = new Random(42);
        times = new int[N];
        for (int i = 0; i < N; i++) times[i] = rnd.nextInt(86_400);
    }

    // 0 or 1, derived from all three fields. XOR-ing it into the next input creates the dependency
    // and keeps the value inside [0..86399] because 86400 is even.
    private static int dep(int packed) {
        return (packed ^ (packed >>> 6) ^ (packed >>> 12)) & 1;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_baseline() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += times[i];
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_baseline() {
        int p = 0;
        for (int i = 0; i < N; i++) {
            int t = times[i] ^ dep(p);
            p = t;
        }
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_javaTime() {
        int acc = 0;
        for (int i = 0; i < N; i++) {
            LocalTime lt = LocalTime.ofSecondOfDay(times[i]);
            acc += TimeOfDay.pack(lt.getHour(), lt.getMinute(), lt.getSecond());
        }
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_javaTime() {
        int p = 0;
        for (int i = 0; i < N; i++) {
            LocalTime lt = LocalTime.ofSecondOfDay(times[i] ^ dep(p));
            p = TimeOfDay.pack(lt.getHour(), lt.getMinute(), lt.getSecond());
        }
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_traditional1() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.traditional1(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_traditional1() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.traditional1(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_traditional2() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.traditional2(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_traditional2() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.traditional2(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_traditional3() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.traditional3(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_traditional3() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.traditional3(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v1() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v1(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v1() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v1(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v1Fixed() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v1Fixed(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v1Fixed() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v1Fixed(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v2() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v2(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v2() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v2(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v3() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v3(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v3() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v3(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v2x64() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v2x64(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v2x64() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v2x64(times[i] ^ dep(p));
        return p;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int tp_v3x64() {
        int acc = 0;
        for (int i = 0; i < N; i++) acc += TimeOfDay.v3x64(times[i]);
        return acc;
    }

    @Benchmark
    @OperationsPerInvocation(N)
    public int lat_v3x64() {
        int p = 0;
        for (int i = 0; i < N; i++) p = TimeOfDay.v3x64(times[i] ^ dep(p));
        return p;
    }
}
