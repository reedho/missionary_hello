# 08 — Sharing: `memo`, `stream`, `signal`

> Story: [`stories/ch08_sharing.clj`](../stories/ch08_sharing.clj) — `clj -M:08`

A task or flow is **unicast by default** — each subscriber kicks off its own process. `memo`, `stream`, and `signal` are the three publisher constructors that share a single underlying process across N subscribers.

## `memo` — share a task's result (run once)

```clojure
(def expensive
  (m/memo
    (m/sp
      (println "  → computing...")
      (m/? (m/sleep 100))
      :done)))

(m/? (m/join vector expensive expensive expensive))
;; only prints "  → computing..." once
```

`memo` is the classic *cache stampede protection*: N concurrent calls share the in-flight computation instead of duplicating it. Effect's equivalent is `Effect.cached`.

When the last subscriber unsubscribes before the task finishes, missionary cancels the underlying task — so `memo` is also a refcount.

## `stream` — share a *discrete* flow

```clojure
(def >clock
  (m/stream
    (m/ap (loop [n 0]
            (m/amb (m/? (m/sleep 100 n))
                   (recur (inc n)))))))

(m/? (m/join vector
       (m/reduce conj (m/eduction (take 3) >clock))
       (m/reduce conj (m/eduction (take 5) >clock))))
;; ⇒ [[0 1 2] [0 1 2 3 4]]   — both subscribers see the same emissions
```

Important: `stream` is *backpressure-aware*. The producer advances at the rate of the **slowest** subscriber, so no subscriber drops events. New subscribers join mid-stream (don't see history).

## `signal` — share a *continuous* flow

We met `signal` in chapter 07. Recap on the sharing axis:

- `signal` is hot, multicast, conflating. New subscribers receive the **current** value immediately and then any subsequent updates.
- Use for state: counters, configuration, mouse position, last-known price.

## When to reach for what

| You have… | You want… | Use |
|:----------|:----------|:----|
| A task | Run it once, share result | `m/memo` |
| A discrete flow | All subscribers see all events | `m/stream` |
| A continuous flow | All subscribers see latest value | `m/signal` |

## Naming convention

In Leo's examples you'll see prefixes encoding the shape:

| Prefix | Means | Example |
|:-------|:------|:--------|
| `>x`   | discrete flow | `>clock`, `>events` |
| `<x`   | continuous flow / signal | `<count`, `<state` |
| `!x`   | mutable reference (atom/ref) | `!input` |

Adopting these helps a lot when reading missionary code in the wild.
