(ns stories.ch10-testing
  "10 — Demoing the testing idioms. The real assertions live in
   test/stories/ch10_testing_test.clj — run with `clj -M:test`.

   This story shows the same patterns as a runnable script.

   Run with:  clj -M:10"
  (:require [missionary.core :as m]))

;; ─── 1. Sync assertion on a task's return value ──────────────────────────────

(defn ex-sync []
  (println "1) m/? makes a task synchronous on the JVM:")
  (let [actual (m/? (m/sleep 5 :ok))]
    (println "   expected :ok  actual" actual
             (if (= :ok actual) "  ✓" "  ✗"))))

;; ─── 2. Asserting on failure ─────────────────────────────────────────────────

(defn ex-failure []
  (println "2) catching ex-info from m/sp:")
  (let [caught (try (m/? (m/sp (throw (ex-info "boom" {}))))
                    (catch clojure.lang.ExceptionInfo e (ex-message e)))]
    (println "   expected \"boom\"  actual" (pr-str caught)
             (if (= "boom" caught) "  ✓" "  ✗"))))

;; ─── 3. attempt — failure as a value ─────────────────────────────────────────

(defn ex-attempt []
  (println "3) m/attempt wraps result/error in a thunk:")
  (let [thunk (m/? (m/attempt (m/sp (throw (ex-info "boom" {})))))]
    (try (thunk)
         (catch clojure.lang.ExceptionInfo e
           (println "   thunk replays the failure:" (ex-message e))))))

;; ─── 4. Flow assertion via reduce ────────────────────────────────────────────

(defn ex-flow []
  (println "4) reduce a flow to a value for assertion:")
  (let [actual (m/? (m/reduce conj
                      (m/eduction (filter odd?) (m/seed (range 10)))))]
    (println "   expected [1 3 5 7 9]  actual" actual
             (if (= [1 3 5 7 9] actual) "  ✓" "  ✗"))))

(defn -main [& _]
  (println "=== ch10 — testing patterns ===")
  (ex-sync)
  (ex-failure)
  (ex-attempt)
  (ex-flow)
  (println "\nRun the real test suite with:  clj -M:test")
  (shutdown-agents))
