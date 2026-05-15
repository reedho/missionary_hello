# 01 — Missionary basics

> Story: [`stories/ch01_basics.clj`](../stories/ch01_basics.clj) — run with `clj -M:01`

## Mental model

A **task** is a value that describes an action which eventually terminates with success or failure. Two key properties:

1. **Lazy.** Nothing happens on construction. You hand the task two callbacks (`success`, `failure`) and *then* it runs.
2. **Cancellable.** Running a task returns a `cancel!` thunk. Call it to interrupt the action.

In code, a task is literally a function: `(fn [success failure] cancel!)`. Most of the time you don't see this shape because you compose tasks with macros (`sp`) and operators (`join`, `race`, …).

## Sequential process — `sp` + `?`

`sp` (sequential process) wraps a body of Clojure code as a task. Inside `sp`, `m/?` *parks* the current evaluation until an inner task completes:

```clojure
(def slowmo-hello
  (m/sp
    (println "Hello")
    (m/? (m/sleep 500))
    (println "World")))

(m/? slowmo-hello)
;; "Hello"  →  500ms  →  "World"  →  returns nil
```

Read `(m/? task)` as "await this task and return its result". On failure, `m/?` rethrows.

## Constructors you'll use day-to-day

- `(m/sp …body…)` — wrap a body as a task
- `(m/sleep ms)` / `(m/sleep ms value)` — task delaying for `ms`
- `m/never` — a task that never completes (cancellable)
- A plain value can't be a task — wrap it: `(m/sp value)` or `(constantly task)` for trivial cases.

## Running a task — three options

```clojure
;; 1) Block the current thread (REPL / main thread on JVM):
(m/? my-task)

;; 2) Manually with two callbacks — returns a cancel! thunk:
(def cancel! (my-task #(prn ::ok %) #(prn ::err %)))
;; …later…
(cancel!)

;; 3) Bridge to a CompletableFuture / Promise:
(defn cf! [task]
  (let [cf (java.util.concurrent.CompletableFuture.)]
    (task #(.complete cf %) #(.completeExceptionally cf %)) cf))
```

On ClojureScript, option 1 doesn't work (no blocking) — use option 2 with a JS promise wrapper.

## Failure is an exception

Inside `sp`, failure is an *ordinary* `throw`. There's no separate "fail channel":

```clojure
(m/? (m/sp
       (throw (ex-info "Boom" {:why :demo}))))
;; ↳ throws ExceptionInfo at the call site
```

This keeps the host language — `try`/`catch`/`finally` works the way you'd expect inside `sp`. The shift from Effect-TS is that there's no typed `E` channel; you discriminate failures by `ex-data` or `class`.

## What's *not* like an Effect

| Effect-TS | Missionary | Note |
|:----------|:-----------|:-----|
| `Effect<A, E, R>` | `task` | No typed `E`, no `R` channel — pass deps as args / dynamic vars |
| `Effect.gen` + `yield*` | `m/sp` + `m/?` | Same idea: rewrite body in async style |
| `Effect.runPromise` | `m/?` (JVM) / wrap in JS promise (CLJS) | Top-level "run" |

## Try it

```bash
clj -M:01
```
