# 09 — Blocking I/O & callback bridges

> Story: [`stories/ch09_blocking_and_observe.clj`](../stories/ch09_blocking_and_observe.clj) — `clj -M:09`

Two real-world necessities: getting blocking JVM calls out of missionary's hot path, and bridging callback-style APIs into a flow.

## `m/via` — offload to an executor

Missionary runs user code on the **calling thread** for efficiency. That's fine for in-memory work, but `Thread/sleep`, JDBC calls, `slurp`, HTTP-client `send`, etc. would stall the whole graph. Two predefined pools:

- `m/blk` — cached pool for **blocking I/O** (HTTP, files, sockets, JDBC)
- `m/cpu` — fork-join pool for **CPU-bound** work

```clojure
(m/sp
  (let [body (m/? (m/via m/blk (slurp "https://example.com")))]
    (count body)))
```

`m/via` returns a task that runs the body on the chosen executor and parks the surrounding `sp` until it completes.

**Rule of thumb:** if a JVM call would block a thread for more than ~1 ms, wrap it in `(m/via m/blk …)`. CLJS has no executor (single-threaded), so `m/via` is JVM-only.

Without `m/via`, two concurrent blocking calls serialize on the same thread:

```clojure
(time (m/? (m/join vector (m/sp (Thread/sleep 500))
                          (m/sp (Thread/sleep 500)))))
;; ~1000ms — both bodies run sequentially on the calling thread

(time (m/? (m/join vector (m/via m/blk (Thread/sleep 500))
                          (m/via m/blk (Thread/sleep 500)))))
;; ~500ms — both run concurrently on the blocking pool
```

## `m/observe` — bridge a callback / "firehose" source

Most external sources (DOM events, JMS listeners, JDBC `RowSetListener`, gRPC server-streaming, Kafka consumer) push values via a callback. They have no `request(n)` handshake, so an unbounded firehose can swamp a slow consumer.

`m/observe` wraps a callback source into a **backpressured flow**:

```clojure
(defn clicks-flow [button]
  (m/observe
    (fn [emit!]
      (let [h (fn [e] (emit! e))]
        (.addEventListener button "click" h)
        ;; return cleanup thunk — runs on flow cancellation
        #(.removeEventListener button "click" h)))))
```

The `emit!` callback **blocks the calling thread** while a transfer is pending (so the producer feels backpressure naturally on the JVM). On CLJS it throws if you push while a transfer is pending; you need a buffer in front (`m/buffer`) for high-rate sources.

## `m/observe` termination

`m/observe` produces an **infinite** flow by default. To terminate, either:

1. The consumer cancels (the cleanup thunk runs).
2. The producer signals end-of-stream by closing the source and letting your `emit!` calls stop.

A common pattern is to emit a sentinel and filter on it:

```clojure
(->> source-flow
     (m/eduction (take-while some?))   ;; nil = sentinel meaning "done"
     (m/reduce conj)
     (m/?))
```

## Putting it together

```clojure
;; Fetch N URLs concurrently on the blocking pool, collect bodies
(defn fetch-many [urls]
  (m/sp
    (m/? (m/reduce conj
           (m/ap (let [url (m/?> ##Inf (m/seed urls))]
                   (m/? (m/via m/blk (slurp url)))))))))
```

This is the missionary idiom for "concurrent HTTP fan-out": `ap` + `?> ##Inf` for fan-out, `via m/blk` for the blocking I/O itself.

## Summary

| Need | Tool | Effect-TS analogue |
|:-----|:-----|:-------------------|
| Run blocking JVM call without stalling | `(m/via m/blk …)` | `Effect.blocking` (semantically) |
| Run CPU-heavy work off the main loop | `(m/via m/cpu …)` | `Effect.runCallback` + Worker (manual) |
| Wrap a callback source as a flow | `(m/observe init)` | `Stream.async` / `Stream.asyncScoped` |
| Apply backpressure to a hot source | `m/observe` + `m/buffer` | `Stream.buffer` |
