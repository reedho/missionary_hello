# missionary_hello

A hands-on Clojure storybook for [Missionary](https://github.com/leonoel/missionary) — Leo Noel's functional effect & streaming system. Each chapter is a small, runnable namespace paired with a short tutorial under [`docs/`](./docs).

Companion to [`effectts_hello`](../effectts_hello) (the Effect-TS storybook). Same spirit, different language. If you've worked through the Effect-TS one and want the Clojure parallel, you're in the right place.

> Missionary version: **`b.47`**

## Run a chapter

```bash
clj -M:01     # chapter 1, etc.
clj -M:02
...
clj -M:10

clj -M:test   # run the test suite
```

REPL-driven works too:

```bash
clj
;; then in the REPL:
(require '[stories.ch01-basics :as ch01])
(ch01/-main)
```

## Chapters

| #  | Story                                  | What you learn                                                                |
| -- | -------------------------------------- | ----------------------------------------------------------------------------- |
| 01 | `ch01-basics`                          | `sp`, `?`, `sleep`, running a task, the `(s f) -> cancel` shape                |
| 02 | `ch02-combinators`                     | `join`, `race`, `any`, `all`, `timeout`, `attempt`/`absolve`                  |
| 03 | `ch03-cancellation`                    | `!`, `Cancelled`, `compel`, dispose function, `try`/`finally`                 |
| 04 | `ch04-communication`                   | `dfv`, `mbx`, `rdv`, `sem`, `holding`                                         |
| 05 | `ch05-flow-basics`                     | `seed`, `reduce`, `eduction`, `reductions`, `zip`                             |
| 06 | `ch06-ambiguous`                       | `ap`, `?>`, `?<`, `amb`/`amb=`, concurrent fan-out, debounce                  |
| 07 | `ch07-signals`                         | `cp`, `watch`, `signal`, `latest` — glitch-free reactive DAG                  |
| 08 | `ch08-sharing`                         | `memo`, `stream`, `signal` as publishers; shared clock                        |
| 09 | `ch09-blocking-and-observe`            | `via m/blk` for blocking I/O; `observe` to bridge callback APIs               |
| 10 | `ch10-testing`                         | testing tasks/flows with `clojure.test` + `m/?`                               |

## Mental model

A Missionary **task** is literally a function `(success-cb failure-cb) -> cancel-fn`. It's a value, lazy, cancellable. You compose them with `sp` (sequential), `ap` (ambiguous / flow), or `cp` (continuous), and let the supervision tree handle failure propagation, cancellation, and resource cleanup.

A **flow** is the multi-value extension: it produces an arbitrary number of values with backpressure. Discrete flows (events) and continuous flows (signals) share the same protocol but differ in sampling semantics.

The [`docs/`](./docs) tutorials explain each piece. The story files show it running.

## Cross-reference

- Effect-TS side-by-side: `effectts_hello/docs/missionary-vs-effect.md`
- Upstream: <https://github.com/leonoel/missionary>
- API: <https://cljdoc.org/d/missionary/missionary/CURRENT/api/missionary.core>
- Hello task: <https://github.com/leonoel/missionary/blob/master/doc/tutorials/hello_task.md>
- Hello flow: <https://github.com/leonoel/missionary/blob/master/doc/tutorials/hello_flow.md>
