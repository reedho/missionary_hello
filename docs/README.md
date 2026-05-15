# Missionary Tutorials

Short, focused tutorials — one per story. Read top-to-bottom the first time; after that, jump to whichever chapter covers the problem you have.

Each tutorial pairs with a runnable namespace in [`../stories/`](../stories/). The doc explains the *why*; the code shows the *how*. Run a chapter with `clj -M:NN`.

## Chapters

1. [Basics](./01-basics.md) — `sp`, `?`, `sleep`, the shape of a task
2. [Task combinators](./02-combinators.md) — `join`, `race`, `any`, `all`, `timeout`, `attempt`
3. [Cancellation](./03-cancellation.md) — `!`, `Cancelled`, `compel`, `dispose!`, `try`/`finally`
4. [Communication](./04-communication.md) — `dfv`, `mbx`, `rdv`, `sem`, `holding`
5. [Flow basics](./05-flow-basics.md) — `seed`, `reduce`, `eduction`, `reductions`, `zip`
6. [Ambiguous evaluation](./06-ambiguous.md) — `ap`, `?>`, `?<`, `amb`/`amb=`, concurrent forking, debounce
7. [Signals (FRP)](./07-signals.md) — `cp`, `watch`, `signal`, `latest`, glitch-free DAG
8. [Sharing](./08-sharing.md) — `memo`, `stream`, `signal` as publishers
9. [Blocking I/O & observe](./09-blocking-and-observe.md) — `via m/blk`, `observe` for callback APIs
10. [Testing](./10-testing.md) — `clojure.test` + `m/?` patterns

## Conventions

- `m` is the alias for `missionary.core` in every story.
- All runnable demos use `m/?` on the JVM main thread, so each chapter is a simple `clj -M:NN` away.
- `dispose!` (the thunk returned when you run a task) is shown explicitly where it matters — that's how cancellation is triggered from outside.
