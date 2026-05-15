# 06 — Ambiguous evaluation (`ap`)

> Story: [`stories/ch06_ambiguous.clj`](../stories/ch06_ambiguous.clj) — `clj -M:06`

This is missionary's signature trick. `ap` (ambiguous process) is a macro that *looks* like `sp` but builds a **flow** instead of a single value. Inside `ap`, `?>` is a fork point: pull one value, evaluate the body to the end, then *backtrack* to the fork and pull the next.

## Pull and backtrack — `?>`

```clojure
(m/? (m/reduce conj
       (m/ap (let [x (m/?> (m/seed [1 2 3]))]
               (str "n=" x)))))
;; ⇒ ["n=1" "n=2" "n=3"]
```

Read it: "for each value `x` pulled from the flow, evaluate the body and emit the result". The body runs **sequentially** for each value (with `par=1`, the default).

## Concurrent forking — `(?> par flow)`

Give `?>` an extra `par` argument and N forks run **concurrently**:

```clojure
(m/? (m/reduce conj
       (m/ap (let [ms (m/?> ##Inf (m/seed [300 100 400 200]))]
               (m/? (m/sleep ms ms))))))
;; ⇒ [100 200 300 400]   (in finish order, not seed order)
```

This is the idiomatic parallel fan-out: each value spawns a fork, all forks race, results stream back in completion order. `##Inf` = unlimited; `(m/?> 5 flow)` = bounded to 5.

## Preemptive forking — `?<` (switch)

`?<` is the *latest wins* variant. When the upstream is ready with a new value, the in-flight branch gets **cancelled**, and a new branch starts on the new value. This is the building block of `debounce`, `switchLatest`, "live search" suggestions:

```clojure
(defn debounce [delay flow]
  (m/ap (let [x (m/?< flow)]
          (try (m/? (m/sleep delay x))
               (catch missionary.Cancelled _ (m/amb))))))
```

For each new value `x` from the source, sleep `delay` ms then emit. If a newer value arrives during the sleep, the sleep is cancelled (`Cancelled` is caught) and `m/amb` emits nothing. The net effect: emit only values that aren't followed by another within `delay`.

## `amb`, `amb=` — multiple sub-expressions

Inside `ap`, `amb` lets the body have multiple values:

```clojure
(m/? (m/reduce conj
       (m/ap (m/amb :a :b :c))))
;; ⇒ [:a :b :c]
```

`amb=` runs the expressions concurrently (results come back in finish order). The 0-arg `(m/amb)` emits nothing — useful as a sentinel to drop a value (we saw it in `debounce` above).

## Producing flows from a `loop`

`ap` plays nicely with `loop`/`recur`:

```clojure
(m/? (m/reduce conj
       (m/ap (loop [[x & xs] (range 5)]
               (if x (m/amb x (recur xs)) (m/amb))))))
;; ⇒ [0 1 2 3 4]
```

Use this when the next value depends on the previous, or when you can't materialise the seed up-front.

## Summary

| Form | Effect | Effect-TS analogue |
|:-----|:-------|:-------------------|
| `(m/ap …)` + `(m/?> f)` | Build a flow; sequential fan-out | `Stream.flatMap(…, { concurrency: 1 })` |
| `(m/ap …)` + `(m/?> n f)` | Bounded concurrent fan-out | `Stream.flatMap(…, { concurrency: n })` |
| `(m/ap …)` + `(m/?> ##Inf f)` | Unbounded fan-out | `Stream.flatMap(…, { concurrency: "unbounded" })` |
| `(m/?< flow)` | Preemptive: latest cancels in-flight | `Stream.switchMap` |
| `(m/amb …)` | Sequential alternation | `Stream.concat` of constants |
| `(m/amb= …)` | Concurrent alternation | `Stream.merge` of constants |
| `(m/amb)` | Drop / emit nothing | `Stream.empty` |
