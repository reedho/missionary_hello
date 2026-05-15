(ns stories.ch08-sharing
  "08 — Sharing: memo (task), stream (discrete flow), signal (continuous flow).

   Run with:  clj -M:08"
  (:require [missionary.core :as m]))

;; ─── 1. memo — share one task across N subscribers ───────────────────────────

(defn ex-memo []
  (println "1) memo — three concurrent waiters, one underlying computation:")
  (let [hits (atom 0)
        expensive (m/memo
                    (m/sp
                      (swap! hits inc)
                      (m/? (m/sleep 50))
                      :done))
        result (m/? (m/join vector expensive expensive expensive))]
    (println "   ⇒" result "  with" @hits "computation(s)")))

;; ─── 2. stream — share a discrete flow ───────────────────────────────────────

(defn ex-stream []
  (println "2) stream — shared discrete clock, both subscribers see same ticks:")
  (let [>clock (m/stream
                 (m/ap (loop [n 0]
                         (m/amb (m/? (m/sleep 30 n))
                                (recur (inc n))))))
        r (m/? (m/join vector
                 (m/reduce conj (m/eduction (take 3) >clock))
                 (m/reduce conj (m/eduction (take 5) >clock))))]
    (println "   ⇒" r)))

;; ─── 3. signal — late subscribers see current value (replay-1) ───────────────

(defn ex-signal-replay []
  (println "3) signal — late subscriber receives current value immediately:")
  (let [!v   (atom 0)
        <v   (m/signal (m/watch !v))]
    ;; eagerly subscribe so the signal is hot
    (let [early (future (m/? (m/reduce conj (m/eduction (take 3) <v))))]
      (Thread/sleep 20) (swap! !v inc)
      (Thread/sleep 20) (swap! !v inc)
      ;; subscribe late — already at 2; take 1 → just sees [2]
      (let [late (future (m/? (m/reduce conj (m/eduction (take 1) <v))))]
        (println "   early ⇒" @early "   late ⇒" @late)))))

(defn -main [& _]
  (println "=== ch08 — sharing ===")
  (ex-memo)
  (ex-stream)
  (ex-signal-replay)
  (shutdown-agents))
