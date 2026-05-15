(ns stories.ch01-basics
  "01 — Missionary basics.
   sp, ?, sleep, the (success, failure) -> cancel! shape, and how failure works.

   Run with:  clj -M:01"
  (:require [missionary.core :as m]))

;; ─── 1. The simplest task ────────────────────────────────────────────────────
;; (m/sp ...) wraps a body in a task. The task is a *value*, not an action yet.

(def hello (m/sp "Hello from Missionary"))
;; `hello` is a function: (fn [success failure] cancel!)

;; ─── 2. Three ways to run a task ─────────────────────────────────────────────

(defn ex-run-blocking []
  (println "1) m/? blocks the REPL/main thread:" (m/? hello)))

(defn ex-run-callbacks []
  (println "2) callback form returns a cancel! thunk:")
  (let [cancel! (hello #(println "   success ⇒" %)
                       #(println "   failure ⇒" %))]
    ;; for a fast task there's nothing to cancel — just demonstrate the shape
    (cancel!)))

(defn ex-run-completable-future []
  (println "3) bridge to CompletableFuture:")
  (let [cf (java.util.concurrent.CompletableFuture.)]
    (hello #(.complete cf %) #(.completeExceptionally cf %))
    (println "   .get ⇒" (.get cf))))

;; ─── 3. Sequential composition with `m/?` inside `m/sp` ──────────────────────

(defn ex-sequential []
  (let [task (m/sp
               (println "   Hello")
               (m/? (m/sleep 200))
               (println "   World")
               (m/? (m/sleep 200))
               (println "   !"))]
    (println "4) sequential — `m/?` parks until the inner task completes:")
    (m/? task)))

;; ─── 4. Failure is just an exception inside sp ───────────────────────────────

(defn ex-failure []
  (println "5) failure inside sp throws normally:")
  (try
    (m/? (m/sp (throw (ex-info "Boom" {:why :demo}))))
    (catch clojure.lang.ExceptionInfo e
      (println "   caught:" (ex-message e) (ex-data e)))))

;; ─── 5. m/sleep with a return value ──────────────────────────────────────────

(defn ex-sleep-returns []
  (println "6) (m/sleep ms value) returns `value` after the delay:")
  (println "   ⇒" (m/? (m/sleep 100 :ready))))

(defn -main [& _]
  (println "=== ch01 — basics ===")
  (ex-run-blocking)
  (ex-run-callbacks)
  (ex-run-completable-future)
  (ex-sequential)
  (ex-failure)
  (ex-sleep-returns)
  (shutdown-agents))
