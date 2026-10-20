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

The full run takes 10 to 15 minutes (3 forks). For a quick check: `-f 1 -wi 2 -i 3`.

The latency benchmarks feed each result into the next input, so nothing can overlap. The throughput
benchmarks run independent conversions. They answer different questions, and a variant can win one and lose
the other. `lat_baseline` and `tp_baseline` are the same loops with the conversion taken out, and
`summarize.py` subtracts them.

`javaTime` calls `LocalTime.ofSecondOfDay` for reference. It allocates a `LocalTime`, so it isn't a fair
comparison.

Two other runs worth doing: `-jvmArgs -XX:-UseSuperWord` on the throughput benchmarks, to see how much the
auto-vectorizer is helping, and `-prof gc` on `tp_javaTime`.

## Results

Not run yet.

## Notes

- Inputs are non-negative seconds. The fixed-point variants have a maximum input (`Verify` checks it), which
  is far beyond one day.
- Unix time only, no leap seconds.
- A conversion costs a few nanoseconds. This only matters if you do a lot of them.
