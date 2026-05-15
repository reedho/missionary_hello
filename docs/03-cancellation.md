# 03 — Cancellation

> Story: [`stories/ch03_cancellation.clj`](../stories/ch03_cancellation.clj) — `clj -M:03`

Missionary's cancellation story is built into the protocol. Every task carries a `cancel!` thunk; siblings of a failing branch get cancelled automatically; CPU-bound loops can ask "have I been cancelled?" with `m/!`.

## The dispose function

Running a task in callback form returns a *cancel function*:

```clojure
(def cancel! ((m/sleep 5000) prn prn))  ;; starts a 5s sleep
(cancel!)                               ;; ⇒ failure callback fires with Cancelled
```

`(m/?)` does the same parking under the hood — when the surrounding `sp` is cancelled, `m/?` raises a `missionary.Cancelled` inside the parked frame.

## Cooperative checks with `m/!`

CPU-bound work can periodically check whether it's been cancelled:

```clojure
(def long-loop
  (m/sp
    (loop [x 0]
      (m/!)               ;; throws missionary.Cancelled if cancelled
      (if (< x Long/MAX_VALUE)
        (recur (inc x))
        x))))
```

If the surrounding context is cancelled, `(m/!)` throws `missionary.Cancelled` and the loop unwinds normally. If you wrap with `try`/`catch` you can convert that signal to a value:

```clojure
(loop [x 0]
  (try (m/!)
       (recur (inc x))
       (catch missionary.Cancelled _ x)))
```

## `try`/`finally` for cleanup

Inside `sp`, `try`/`finally` works exactly as you'd expect — `finally` runs whether the body completed, threw, or was cancelled:

```clojure
(m/sp
  (try
    (println "acquire")
    (m/? do-work)
    (finally
      (println "release"))))   ;; runs even if cancelled mid-work
```

This is how you do resource cleanup. Effect's `Scope`+`acquireRelease` is more formal; missionary's answer is "use the host language's `try`/`finally`, scoped to flow/task lifetime".

## `compel` — make a task uninterruptible

`compel` wraps a task so its cancel signal becomes a no-op. The underlying action will run to completion regardless:

```clojure
(def critical (m/compel (m/sp (m/? (m/sleep 200)) (println "must finish"))))

((critical prn prn))           ;; "must finish" prints even though we called cancel!
```

Use sparingly — by default, missionary cancels promptly and that's what you usually want.

## Propagation, by example

`join` cancels surviving siblings when one branch fails. We saw it in chapter 2 (`slow finished its body? false`). The same applies to `race`, `any`, `timeout`, and any `ap` flow that gets disposed.

## Summary

| Mechanism | What it does |
|:----------|:-------------|
| Calling the dispose thunk | External cancellation trigger |
| `(m/!)` | Cooperative cancellation check inside CPU-bound code |
| `missionary.Cancelled` (class) | The exception raised on cancellation |
| `try`/`finally` in `sp` | Cleanup that runs even on cancellation |
| `(m/compel task)` | Make a region uninterruptible |
| Sibling propagation in `join`/`race`/`any` | Failure in one branch → cancel the rest |
