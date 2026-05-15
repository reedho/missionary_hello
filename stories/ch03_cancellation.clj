(ns stories.ch03-cancellation
  "03 — Cancellation: dispose!, m/!, missionary.Cancelled, try/finally, compel.

   Run with:  clj -M:03"
  (:require [missionary.core :as m]))

;; ─── 1. The dispose function ─────────────────────────────────────────────────

(defn ex-dispose []
  (println "1) calling the dispose thunk cancels a pending task:")
  (let [done? (promise)
        cancel! ((m/sp
                   (try
                     (m/? (m/sleep 5000 :nope))
                     (catch Exception e
                       (deliver done? [:cancelled (class e)]))))
                 (fn [_] (deliver done? [:ok]))
                 (fn [e] (deliver done? [:err (class e)])))]
    (Thread/sleep 50)
    (cancel!)
    (println "   ⇒" @done?)))

;; ─── 2. Cooperative check with m/! ───────────────────────────────────────────

(defn ex-bang []
  (println "2) (m/!) lets a tight loop notice cancellation:")
  (let [result-dfv (m/dfv)
        ;; Run on the CPU pool so the main thread can deliver the cancel signal.
        task (m/via m/cpu
               (loop [x 0]
                 (let [stop? (try (m/!) false
                                  (catch missionary.Cancelled _ true))]
                   (if stop? x (recur (inc x))))))
        cancel! (task #(result-dfv %) #(result-dfv %))]
    (Thread/sleep 5)
    (cancel!)
    (println "   loop counted up to:" (m/? result-dfv))))

;; ─── 3. try/finally runs on cancellation ─────────────────────────────────────

(defn ex-finally []
  (println "3) try/finally always runs — even on cancellation:")
  (let [released (promise)
        cancel! ((m/sp
                   (try
                     (m/? (m/sleep 5000))
                     (finally (deliver released :released))))
                 (constantly nil) (constantly nil))]
    (Thread/sleep 30)
    (cancel!)
    (println "   finally fired?" @released)))

;; ─── 4. compel makes a task uninterruptible ──────────────────────────────────

(defn ex-compel []
  (println "4) compel ignores cancellation — body runs to completion:")
  (let [done    (promise)
        cancel! ((m/compel
                   (m/sp
                     (m/? (m/sleep 200))
                     (deliver done :finished)))
                 (constantly nil) (constantly nil))]
    (Thread/sleep 30)
    (cancel!)                                ;; signalled but ignored
    (println "   compel result:" (deref done 500 :timeout))))

;; ─── 5. Sibling propagation in join (revisits ch02 from the cancel angle) ────

(defn ex-sibling-cancel []
  (println "5) join cancels surviving siblings when one branch fails:")
  (let [released (promise)
        slow     (m/sp
                   (try (m/? (m/sleep 5000))
                        (finally (deliver released :sibling-cleanup))))
        boom     (m/sp (m/? (m/sleep 50)) (throw (ex-info "boom" {})))]
    (try (m/? (m/join vector slow boom))
         (catch Exception _))
    (println "   sibling cleanup ran?" @released)))

(defn -main [& _]
  (println "=== ch03 — cancellation ===")
  (ex-dispose)
  (ex-bang)
  (ex-finally)
  (ex-compel)
  (ex-sibling-cancel)
  (shutdown-agents))
