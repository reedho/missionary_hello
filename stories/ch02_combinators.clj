(ns stories.ch02-combinators
  "02 — Task combinators: join, race, any, all, timeout, attempt/absolve.

   Run with:  clj -M:02"
  (:require [missionary.core :as m]))

;; ─── 1. join — all must succeed ──────────────────────────────────────────────

(defn ex-join []
  (let [t0 (System/currentTimeMillis)
        r  (m/? (m/join vector
                  (m/sleep 200 :a)
                  (m/sleep 100 :b)))]
    (println "1) join ⇒" r "(took" (- (System/currentTimeMillis) t0) "ms)")))

;; ─── 2. join short-circuits on failure ───────────────────────────────────────

(defn ex-join-fails []
  (println "2) join with one failure — siblings get cancelled:")
  (let [survived? (atom false)
        slow      (m/sp
                    (try (m/? (m/sleep 500))
                         (reset! survived? true)
                         (finally (println "   slow finally"))))
        fast-fail (m/sp (m/? (m/sleep 100))
                        (throw (ex-info "boom" {})))]
    (try (m/? (m/join vector slow fast-fail))
         (catch Exception e (println "   caught:" (ex-message e))))
    (println "   slow finished its body?" @survived?)))

;; ─── 3. race vs any ──────────────────────────────────────────────────────────

(defn ex-race []
  (println "3) race — first SUCCESS wins:" (m/? (m/race (m/sleep 200 :slow)
                                                        (m/sleep 100 :fast)))))

(defn ex-any []
  (println "4) any — first COMPLETION wins (success or failure):")
  (try
    (m/? (m/any (m/sleep 200 :slow)
                (m/sp (m/? (m/sleep 100))
                      (throw (ex-info "boom" {})))))
    (catch Exception e (println "   the failure won:" (ex-message e)))))

;; ─── 4. all — settle every task, no short-circuit ────────────────────────────

(defn ex-all []
  (println "5) all — Promise.allSettled style:")
  (let [combine (fn [f g]
                  [(try (f) (catch Exception _ :a-failed))
                   (try (g) (catch Exception _ :b-failed))])
        result  (m/? (m/all combine
                       (m/sp (m/? (m/sleep 100)) :a)
                       (m/sp (m/? (m/sleep 50))
                             (throw (ex-info "boom" {})))))]
    (println "   ⇒" result)))

;; ─── 5. timeout ──────────────────────────────────────────────────────────────

(defn ex-timeout []
  (println "6) timeout — cancel after ms, optional fallback value:")
  (println "   slow,100ms       ⇒" (m/? (m/timeout (m/sleep 200 :a) 100)))
  (println "   slow,100ms,:b    ⇒" (m/? (m/timeout (m/sleep 200 :a) 100 :b)))
  (println "   fast,200ms       ⇒" (m/? (m/timeout (m/sleep 100 :a) 200))))

;; ─── 6. attempt / absolve — failure as a value ───────────────────────────────

(defn ex-attempt []
  (println "7) attempt wraps the result/error in a thunk:")
  (let [thunk (m/? (m/attempt (m/sp (throw (ex-info "boom" {})))))]
    (println "   thunk type:" (type thunk))
    (try (thunk)
         (catch Exception e (println "   replay throws:" (ex-message e))))))

(defn ex-absolve []
  (println "8) absolve unwraps a thunk-task back to its value:")
  (println "   ⇒" (m/? (m/absolve (m/attempt (m/sp :ok))))))

(defn -main [& _]
  (println "=== ch02 — combinators ===")
  (ex-join)
  (ex-join-fails)
  (ex-race)
  (ex-any)
  (ex-all)
  (ex-timeout)
  (ex-attempt)
  (ex-absolve)
  (shutdown-agents))
