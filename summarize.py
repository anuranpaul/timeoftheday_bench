#!/usr/bin/env python3
"""
Turn JMH's JSON output into the table the article uses:
  - subtract the baseline loop (so you see the cost of the conversion itself)
  - show each algorithm relative to Traditional 1 (the java.time-style sequential approach)

Usage:  python3 summarize.py results.json
"""
import json, sys, re

ORDER = ["traditional1", "traditional2", "traditional3", "v1", "v1Fixed",
         "v2", "v3", "v2x64", "v3x64", "javaTime"]
LABEL = {
    "traditional1": "Traditional 1 (sequential, java.time style)",
    "traditional2": "Traditional 2 (three independent)",
    "traditional3": "Traditional 3 (backwards, .NET style)",
    "v1":           "New V1 (parallel, plain division)",
    "v1Fixed":      "New V1 fixed-point",
    "v2":           "New V2 (hi/low bits, 32-bit)",
    "v3":           "New V3 (base-64 trick)",
    "v2x64":        "New V2 64-bit",
    "v3x64":        "New V3 64-bit",
    "javaTime":     "java.time.LocalTime (reference; allocates)",
}

def load(path):
    with open(path) as f:
        data = json.load(f)
    out = {}
    for r in data:
        name = r["benchmark"].split(".")[-1]          # e.g. lat_v3
        m = re.match(r"(tp|lat)_(\w+)$", name)
        if not m:
            continue
        mode, algo = m.groups()
        pm = r["primaryMetric"]
        out[(mode, algo)] = (pm["score"], pm.get("scoreError", 0.0), pm["scoreUnit"])
    return out

def table(res, mode, title):
    base = res.get((mode, "baseline"))
    if base is None:
        print(f"[{title}] no baseline found, skipping")
        return
    ref = res.get((mode, "traditional1"))
    if ref is None:
        print(f"[{title}] no traditional1 found, skipping")
        return
    ref_net = ref[0] - base[0]
    print(f"\n{title}   (baseline loop = {base[0]:.3f} {base[2]}, subtracted)")
    print(f"{'algorithm':46} {'raw ns/op':>10} {'net ns/op':>10} {'+/- raw':>8} {'vs Trad.1':>10}")
    print("-" * 90)
    for a in ORDER:
        if (mode, a) not in res:
            continue
        score, err, unit = res[(mode, a)]
        net = score - base[0]
        ratio = net / ref_net if ref_net > 0 else float("nan")
        print(f"{LABEL[a]:46} {score:10.3f} {net:10.3f} {err:8.3f} {ratio:9.2f}x")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    res = load(sys.argv[1])
    table(res, "lat", "LATENCY (dependent chain)")
    table(res, "tp",  "THROUGHPUT (independent conversions)")
    print("\nSmaller is faster. Ratios below 1.00x mean faster than Traditional 1.")
    print("Net values near zero or negative mean the conversion cost is within the noise of the loop itself.")
