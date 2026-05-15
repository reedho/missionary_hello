# 10 — Testing missionary code

> Story: [`stories/ch10_testing.clj`](../stories/ch10_testing.clj) — `clj -M:10`
> Tests:  [`test/stories/ch10_testing_test.clj`](../test/stories/ch10_testing_test.clj) — `clj -M:test`

There's no dedicated "missionary-test" library — and you don't really need one. Missionary tasks are values; you assert on the value they produce. On the JVM, `m/?` blocks the calling thread, so a deftest body that runs a task with `m/?` works exactly like any other synchronous test.

## The basic shape

```clojure
(ns my.thing-test
  (:require [clojure.test :refer [deftest is]]
            [missionary.core :as m]))

(deftest sleep-returns-its-value
  (is (= :ok (m/? (m/sleep 10 :ok)))))
```

That's it. `m/?` makes the test synchronous; `is` runs against the return value.

## Asserting on failure

`m/?` rethrows on failure. Catch and inspect, or use `is (thrown? …)`:

```clojure
(deftest fails-with-ex-info
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"boom"
        (m/? (m/sp (throw (ex-info "boom" {})))))))
```

If you want failure-as-a-value (more like Effect's `Exit`), wrap with `m/attempt`:

```clojure
(deftest captured
  (let [thunk (m/? (m/attempt (m/sp (throw (ex-info "boom" {})))))]
    (is (thrown? clojure.lang.ExceptionInfo (thunk)))))
```

## Testing flows

Reduce to a vector, compare:

```clojure
(deftest seed-sums-correctly
  (is (= 45 (m/? (m/reduce + (m/seed (range 10))))))
  (is (= [0 1 4 9 16] (m/? (m/reduce conj
                              (m/eduction (map #(* % %))
                                          (m/seed (range 5))))))))
```

## Cancellation under test

For cancellation, run the task in callback form, trigger cancel, then assert on what happened. The story file shows this with a counter loop.

## Timing-sensitive tests

The big trap: tests that rely on wall-clock timing (sleeps, debounce windows, signal propagation) get **flaky** on slow CI. Two ways to reduce that:

1. Use generous windows (50–100ms) — costs you real time but stays robust.
2. Inject a clock: pass `m/sleep` (or your own clock fn) as an argument so tests can swap in a controllable substitute. Missionary doesn't ship a `TestClock` like Effect, so this is on you.

The example in `ch10_testing_test.clj` keeps things simple and just uses real sleeps with generous windows.

## Running the suite

```bash
clj -M:test
```

That's the `cognitect.test-runner` alias from `deps.edn`. It picks up everything under `test/` matching `*-test.clj`.

## Notes

- On **ClojureScript**, `m/?` doesn't block. Use `cljs.test`'s `async` form and wire the callbacks manually, or use a Promise bridge (`(js/Promise. task)`).
- For **property-based tests** (`test.check`), tasks are values — you can generate them, compose, and run, but generating *meaningful* tasks is non-trivial. Stick with example tests unless you have a strong reason.
