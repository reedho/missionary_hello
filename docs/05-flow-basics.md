# 05 — Flow basics

> Story: [`stories/ch05_flow_basics.clj`](../stories/ch05_flow_basics.clj) — `clj -M:05`

A **flow** is a value representing a process that emits 0..N values before terminating. Like tasks, flows are lazy, cancellable, and backpressured. Unlike tasks they have multiple results.

The two flavors of flow:

- **discrete** — emits *events* (HTTP responses, log lines, clicks). Lossless; backpressured by buffering or pausing the producer.
- **continuous** — emits *state* (mouse position, config, current price). Lossy; old values get discarded if the consumer is slow.

This chapter covers discrete flows. Continuous flows / signals get their own chapter (07).

## Building flows

```clojure
(m/seed (range 5))         ;; finite flow from a collection
m/none                     ;; the empty flow
```

## Consuming flows

`m/reduce` turns a flow into a task that folds:

```clojure
(m/? (m/reduce + (m/seed (range 10))))   ;; ⇒ 45
```

`m/reductions` is a flow of running aggregates (like `clojure.core/reductions`):

```clojure
(m/? (m/reduce conj
       (m/reductions + 0 (m/seed [1 2 3 4]))))
;; ⇒ [0 1 3 6 10]
```

`m/eduction` applies transducers to a flow with **zero intermediate allocation** (no per-stage object wrapping):

```clojure
(m/? (m/reduce conj
       (m/eduction (filter odd?) (map #(* % %))
                   (m/seed (range 10)))))
;; ⇒ [1 9 25 49 81]
```

## Combining flows

```clojure
(m/? (m/reduce conj
       (m/zip vector
              (m/seed [:a :b :c])
              (m/seed [1  2  3]))))
;; ⇒ [[:a 1] [:b 2] [:c 3]]
```

`m/zip` combines element-wise; both flows advance in lockstep. Stops when the shorter one ends.

## Bridging callback APIs — `m/observe`

When a value source pushes (no `request(n)` handshake), wrap it with `m/observe`:

```clojure
(def clicks
  (m/observe
    (fn [emit!]
      (let [handler (fn [e] (emit! e))]
        (.addEventListener button "click" handler)
        ;; the returned thunk is the cleanup
        #(.removeEventListener button "click" handler)))))
```

`emit!` is the "push a value" callback. The function you pass to `observe` should return a cleanup thunk that runs when the flow process is cancelled.

## Summary

| Flow piece | Purpose | Effect-TS analogue |
|:-----------|:--------|:-------------------|
| `(m/seed coll)` | Flow from a finite collection | `Stream.fromIterable` |
| `m/none` | Empty flow | `Stream.empty` |
| `(m/reduce rf f)` | Fold to a task | `Stream.runFold` |
| `(m/reductions rf f)` | Running aggregates | `Stream.scan` |
| `(m/eduction xf… f)` | Transduce | `Stream.pipeThrough` / `mapChunks` |
| `(m/zip g & fs)` | Combine element-wise | `Stream.zipWith` |
| `(m/observe init)` | Wrap a callback source | `Stream.async` |
