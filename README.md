# timeofday-bench

I read Ben Joffe's post on converting a timestamp to hour/minute/second faster
(https://www.benjoffe.com/fast-time-of-day) and wanted to see what happens in Java, where the JIT sits
between your code and the CPU. His numbers are from C/C++. I didn't know how much of the gain would survive.

The idea, briefly: the usual approach computes hour, then the remainder, then minute, then second, so every
step waits on the one before it. Computing total minutes and the hour straight from the input lets the CPU
work on them at the same time. The rest of the post builds on that: multiply-shift instead of divide, and a
trick that turns `% 60` into `& 63`.

## What's here

- `TimeOfDay`: the variants (three traditional ones, V1, V1 fixed-point, V2, V3, and 64-bit V2/V3)
- `Verify`: checks every variant against a plain reference for all 86,400 seconds of a day, and checks the
  input ranges the article gives for the fixed-point ones
- `TimeOfDayBench`: JMH benchmarks, latency and throughput
- `summarize.py`: reads JMH's JSON output, subtracts the empty-loop baseline, prints each variant relative to
  traditional 1

Results are packed into one int (`h << 12 | m << 6 | s`), so there's no allocation inside the loops.

## Running it

Needs JDK 18+ (for `Math.unsignedMultiplyHigh`) and Maven.

    mvn -q clean package
    java -cp target/classes dev.anuran.tod.Verify
    java -jar target/benchmarks.jar -rf json -rff results.json
    python3 summarize.py results.json

The full run takes ~15 minutes (3 forks). For a quick check: `-f 1 -wi 2 -i 3`.

The latency benchmarks feed each result into the next input, so nothing can overlap. The throughput
benchmarks run independent conversions. They answer different questions, and a variant can win one and lose
the other. `lat_baseline` and `tp_baseline` are the same loops with the conversion taken out, and
`summarize.py` subtracts them.

`javaTime` calls `LocalTime.ofSecondOfDay` for reference. It allocates a `LocalTime`, so it isn't a fair
comparison.

Two other runs worth doing: `-jvmArgs -XX:-UseSuperWord` on the throughput benchmarks, to see how much the
auto-vectorizer is helping, and `-prof gc` on `tp_javaTime`.

## Results

```
Machine:   Apple M2, 16 GB RAM, macOS 15.6 (Darwin 25.6.0 arm64)
JDK:       OpenJDK 64-Bit Server VM 21.0.10 (Homebrew build 21.0.10)
Command:   java -jar target/benchmarks.jar -rf json -rff results.json

LATENCY (dependent chain)   (baseline loop = 1.757 ns/op, subtracted)
algorithm                                       raw ns/op  net ns/op  +/- raw  vs Trad.1
------------------------------------------------------------------------------------------
Traditional 1 (sequential, java.time style)         9.088      7.331    0.097      1.00x
Traditional 2 (three independent)                   9.066      7.309    0.077      1.00x
Traditional 3 (backwards, .NET style)               9.052      7.295    0.102      1.00x
New V1 (parallel, plain division)                   6.403      4.646    0.080      0.63x
New V1 fixed-point                                  5.689      3.932    0.047      0.54x
New V2 (hi/low bits, 32-bit)                        5.558      3.802    0.060      0.52x
New V3 (base-64 trick)                              5.466      3.709    0.063      0.51x
New V2 64-bit                                       5.187      3.431    0.263      0.47x
New V3 64-bit                                       4.940      3.183    0.098      0.43x
java.time.LocalTime (reference; allocates)         11.630      9.874    0.092      1.35x

THROUGHPUT (independent conversions)   (baseline loop = 0.298 ns/op, subtracted)
algorithm                                       raw ns/op  net ns/op  +/- raw  vs Trad.1
------------------------------------------------------------------------------------------
Traditional 1 (sequential, java.time style)         1.608      1.310    0.082      1.00x
Traditional 2 (three independent)                   1.925      1.627    0.021      1.24x
Traditional 3 (backwards, .NET style)               1.591      1.292    0.021      0.99x
New V1 (parallel, plain division)                   1.283      0.985    0.012      0.75x
New V1 fixed-point                                  1.084      0.786    0.029      0.60x
New V2 (hi/low bits, 32-bit)                        1.083      0.785    0.006      0.60x
New V3 (base-64 trick)                              1.150      0.851    0.040      0.65x
New V2 64-bit                                       0.949      0.651    0.037      0.50x
New V3 64-bit                                       0.754      0.456    0.025      0.35x
java.time.LocalTime (reference; allocates)          3.105      2.807    0.285      2.14x
```

### Follow-up findings

1. **Auto-vectorization check (`-jvmArgs -XX:-UseSuperWord`)**:
   Turning off SuperWord on `tp_*` yielded virtually identical net timings (e.g. `tp_v3x64` scored `0.441 ns/op` vs `0.456 ns/op`; `tp_traditional1` scored `1.203 ns/op` vs `1.310 ns/op`). C2 does not auto-vectorize the loop; performance gains come directly from instruction-level parallelism (ILP) and cheaper scalar operations on the CPU pipelines.
2. **Allocation overhead in `java.time` (`-prof gc`)**:
   `LocalTime.ofSecondOfDay` allocates 24.0 bytes per operation (`LocalTime` instance on 64-bit JVM with compressed oops) and generates ~7.95 GB/sec of garbage in a single thread, directly explaining its ~2.14x slowdown in throughput.

## Notes

- Inputs are non-negative seconds. The fixed-point variants have a maximum input (`Verify` checks it), which
  is far beyond one day.
- Unix time only, no leap seconds.
- A conversion costs a few nanoseconds. This only matters if you do a lot of them.
