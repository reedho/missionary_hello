# 07 — Signals: continuous flows & glitch-free DAGs

> Story: [`stories/ch07_signals.clj`](../stories/ch07_signals.clj) — `clj -M:07`

This is missionary's killer feature — the one Effect-TS doesn't have a direct equivalent for. A **signal** is a *continuous flow*: a time-varying value sampled on demand. Late subscribers see the current value; slow subscribers miss intermediate ones (conflation).

## `m/watch` — atom-as-signal

```clojure
(def !count (atom 0))
(def >count (m/watch !count))   ;; continuous flow of (deref !count)
```

`>count` emits *every* state of `!count` from the moment it's subscribed.

## `m/signal` — multicast a flow, with replay-1 semantics

A bare `m/watch` is unicast (each subscriber runs its own watcher). Wrap with `m/signal` to share one process across many subscribers, and grant late subscribers immediate access to the current value:

```clojure
(def <count (m/signal (m/watch !count)))
```

By convention, the prefix tells you the kind: `>` for discrete, `<` for continuous.

## `m/latest` — derived signal

`(m/latest f & flows)` is a flow that emits whenever **any** upstream emits — combining the latest values with `f`:

```clojure
(def <sum (m/signal (m/latest + <count <count)))
```

When `!count` changes from 1→2, `<count` emits 2, and `<sum` recomputes to 4. The key property of `signal` here is **glitch freedom**: even when `<count` shows up twice in the DAG, `<sum` recomputes *once* with consistent values — never sees `(+ 1 2) = 3` mid-propagation.

## `cp` — continuous process

`cp` (continuous process) is the macro to build a signal *body*. Inside `cp`, `?<` samples a continuous flow:

```clojure
(def <derived
  (m/cp (let [x (m/?< <count)]
          (* x x))))
```

Each time `<count` ticks, the body re-runs and emits the new derived value.

## Glitch-free DAG demo

```clojure
(def !input (atom 1))
(def <x (m/signal (m/watch !input)))
(def <y (m/signal (m/latest + <x <x)))   ;; diamond: <y depends on <x twice

;; consume:
((m/reduce (fn [_ v] (prn v)) nil <y)
 prn prn)
;; prints 2

(swap! !input inc)   ;; prints 4   (NOT 3 — no inconsistent intermediate state)
```

Without glitch freedom, you'd see 2 → 3 → 4 (one upstream copy of `<x` updating before the other). Missionary's reactor schedules the full propagation atomically.

## Sampling a signal on a discrete event

To convert a signal back to a flow of events ("snapshot when X happens"):

```clojure
(m/ap (m/?< trigger-flow)          ;; wait for discrete event
      (m/?< state-signal))         ;; sample current state
```

This is what powers reactive UIs: render whenever a discrete input event fires, using the latest model state.

## Summary

| Form | Purpose | Effect-TS analogue |
|:-----|:--------|:-------------------|
| `(m/watch !atom)` | Atom state as a continuous flow | `SubscriptionRef.changes` (partial) |
| `(m/signal flow)` | Multicast w/ replay-1 | `SubscriptionRef` (partial) |
| `(m/latest f & fs)` | Derived: combine latest values | `Stream.zipLatest` (partial) |
| `(m/cp …body…)` + `(m/?< s)` | Continuous-process macro | (no direct equivalent) |
| Glitch-free DAG | Atomic propagation | Not available |

If you need *real* glitch-free FRP in TypeScript, you reach outside Effect (Solid signals, MobX, etc.). Inside Clojure, missionary is the answer.
