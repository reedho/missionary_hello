# 04 — Communication primitives

> Story: [`stories/ch04_communication.clj`](../stories/ch04_communication.clj) — `clj -M:04`

When sibling branches in a supervision tree need to exchange data, missionary gives you four small primitives. Each is a *function* (consistent with the rest of the library): you call it with N arguments to interact, or run it as a task.

## `dfv` — dataflow variable (one-shot promise)

```clojure
(let [d (m/dfv)]
  (future (Thread/sleep 100) (d 42))
  (m/? d))                              ;; ⇒ 42
```

- `(d v)` (arity-1) — assign once; later calls ignored.
- `(d s f)` (arity-2) — *task* that completes with the bound value once available.

It's a single-assignment cell. Useful as a one-shot reply channel for request/response patterns through a mailbox.

## `mbx` — mailbox (unbounded async queue)

```clojure
(let [post! (m/mbx)]
  (post! :hello)
  (post! :world)
  (m/? post!))                          ;; ⇒ :hello
```

- `(mbx v)` — push value.
- `(mbx s f)` — task pulling one value.

Combine with a tail-recursive `sp` loop to make an **actor**:

```clojure
(defn actor [behavior]
  (let [self (m/mbx)]
    ((m/sp (loop [b behavior]
             (recur (b self (m/? self)))))
     identity identity)                 ;; ignore success/failure
    self))
```

## `rdv` — rendezvous (synchronous handoff)

A 0-capacity channel: `give` parks until a `take` is ready, and vice versa.

```clojure
(let [r (m/rdv)]
  (future (Thread/sleep 50) (m/? (r :handoff)))   ;; give parks until taken
  (m/? r))                                         ;; ⇒ :handoff
```

- `(r v)` — task that completes with `nil` once a taker is ready.
- `(r s f)` — task that pulls a value once a giver arrives.

Closest to Go's unbuffered channel or core.async's `(chan)`.

## `sem` + `holding` — semaphore

```clojure
(let [permit (m/sem 2)]                 ;; 2 concurrent permits
  (m/? (m/join vector
         (m/holding permit (m/? (m/sleep 100 :a)))
         (m/holding permit (m/? (m/sleep 100 :b)))
         (m/holding permit (m/? (m/sleep 100 :c))))))
;; ⇒ [:a :b :c]   (the third permit waits ~100ms behind one of the first two)
```

- `(m/sem)` — defaults to 1 (mutex).
- `(m/sem n)` — counted semaphore.
- `(m/holding sem & body)` — acquire, run body, release in a `try`/`finally`.

## Summary

| Primitive | Use case | Effect-TS analogue |
|:----------|:---------|:-------------------|
| `m/dfv` | One-shot promise / reply channel | `Deferred<A, E>` |
| `m/mbx` | Unbounded async queue / actor inbox | `Queue.unbounded<A>` |
| `m/rdv` | Synchronous handoff (no buffer) | `Queue.bounded(0)` (handoff) |
| `m/sem` + `m/holding` | Counted concurrency limiter | `Semaphore` |
