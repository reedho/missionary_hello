# 02 — Task combinators

> Story: [`stories/ch02_combinators.clj`](../stories/ch02_combinators.clj) — `clj -M:02`

Once you have tasks, the natural next question is "how do I run several at once?" Missionary's task combinators are a small, sharp set.

## `join` — all must succeed

```clojure
(m/? (m/join vector
       (m/sleep 200 :a)
       (m/sleep 100 :b)))
;; ⇒ [:a :b]   (after 200 ms)
```

If **any** task fails, the others are cancelled and the failure propagates. This is structured concurrency: the parent's lifetime bounds the children.

## `race` — first **success** wins

```clojure
(m/? (m/race
       (m/sleep 200 :slow)
       (m/sleep 100 :fast)))
;; ⇒ :fast
```

Losers get cancelled. If **all** tasks fail, `race` fails (aggregated).

## `any` — first **completion** wins (success or failure)

```clojure
(m/? (m/any
       (m/sleep 200 :slow)
       (m/sp (m/? (m/sleep 100)) (throw (ex-info "boom" {})))))
;; ⇒ throws after 100 ms (the failure won the race)
```

`any` is what you want for "race to a definitive answer"; `race` is what you want for "race for a successful answer".

## `all` — settle every task, never short-circuit

```clojure
(m/? (m/all (fn [f g]
              [(try (f) (catch Exception e :a-failed))
               (try (g) (catch Exception e :b-failed))])
            (m/sleep 100 :a)
            (m/sp (throw (ex-info "boom" {})))))
;; ⇒ [:a :a-failed]
```

The function receives one zero-arg thunk per task; calling a thunk replays the result (return value or rethrow). Closest JS analogue: `Promise.allSettled`.

## `timeout` — race a task against a clock

```clojure
(m/? (m/timeout (m/sleep 200 :a) 100))      ;; ⇒ nil   after 100 ms
(m/? (m/timeout (m/sleep 200 :a) 100 :b))   ;; ⇒ :b    after 100 ms
(m/? (m/timeout (m/sleep 100 :a) 200))      ;; ⇒ :a    after 100 ms
```

On timeout the inner task is **cancelled** before `timeout` returns the fallback.

## `attempt` / `absolve` — failure as a value

`attempt` turns a possibly-failing task into one that always succeeds, returning a *zero-arg thunk*. Call the thunk to replay success or rethrow:

```clojure
(let [wrapped (m/attempt (m/sp (throw (ex-info "boom" {}))))
      result  (m/? wrapped)]
  ;; result is a thunk
  (try (result)
       (catch Exception e (ex-message e))))
;; ⇒ "boom"
```

`absolve` is the inverse: it takes a task whose value is a thunk and unwraps it.

```clojure
(m/? (m/absolve (m/attempt (m/sp :ok))))
;; ⇒ :ok
```

This pair is what `m/all` builds on — and it's how you delay or convert failure into branchable data.

## Summary

| Combinator | Semantics | Effect-TS analogue |
|:-----------|:----------|:-------------------|
| `(m/join f & ts)` | all succeed → `(apply f results)`; any fail → cancel + fail | `Effect.all` |
| `(m/race & ts)` | first success wins; all fail → fail | `Effect.race` / `firstSuccessOf` |
| `(m/any & ts)` | first completion wins (success or failure) | `Effect.raceFirst` |
| `(m/all f & ts)` | settle every task; `f` receives result thunks | `Effect.all` over `Effect.either` |
| `(m/timeout t ms [v])` | cancel after `ms`; return `v` (or nil) | `Effect.timeout` / `timeoutTo` |
| `(m/attempt t)` | wrap result/error in a thunk; always succeeds | `Effect.either` |
| `(m/absolve t)` | unwrap a thunk-returning task | (paired with `Effect.either`) |
