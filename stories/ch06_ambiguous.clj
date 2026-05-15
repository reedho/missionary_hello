(ns stories.ch06-ambiguous
  "06 — Ambiguous evaluation: ap, ?>, ?<, amb, amb=, concurrent fan-out, debounce.

   Run with:  clj -M:06"
  (:require [missionary.core :as m]))

;; ─── 1. Basic ap + ?> — sequential backtracking ──────────────────────────────

(defn ex-basic-ap []
  (println "1) ap + ?> — pull, fork, backtrack:")
  (let [r (m/? (m/reduce conj
                 (m/ap (let [x (m/?> (m/seed [1 2 3]))]
                         (str "n=" x)))))]
    (println "   ⇒" r)))

;; ─── 2. Concurrent fan-out — ?> with `par` arg ───────────────────────────────

(defn ex-fan-out []
  (println "2) ?> with ##Inf — concurrent fan-out (results in finish order):")
  (let [t0 (System/currentTimeMillis)
        r  (m/? (m/reduce conj
                  (m/ap (let [ms (m/?> ##Inf (m/seed [300 100 400 200]))]
                          (m/? (m/sleep ms ms))))))]
    (println "   ⇒" r "  took ~" (- (System/currentTimeMillis) t0) "ms (≈400)")))

;; ─── 3. Bounded fan-out (par = 2) ────────────────────────────────────────────

(defn ex-bounded []
  (println "3) ?> 2 — bounded parallelism, max 2 in flight:")
  (let [t0 (System/currentTimeMillis)
        r  (m/? (m/reduce conj
                  (m/ap (let [ms (m/?> 2 (m/seed [200 200 200 200]))]
                          (m/? (m/sleep ms ms))))))]
    (println "   ⇒" r "  took ~" (- (System/currentTimeMillis) t0) "ms (≈400)")))

;; ─── 4. Preemptive switching — ?< (debounce) ─────────────────────────────────

(defn debounce [delay flow]
  (m/ap (let [x (m/?< flow)]
          (try (m/? (m/sleep delay x))
               (catch missionary.Cancelled _ (m/amb))))))

(defn ex-debounce []
  (println "4) ?< (switch) — debounce: emit only values not followed within 50ms:")
  (let [;; simulated typing: spurts of input, with a pause near the end
        signal (m/ap (let [d (m/?> (m/seed [10 10 10 10 80 10]))]
                       (m/? (m/sleep d (System/currentTimeMillis)))))
        r      (m/? (m/reduce conj (debounce 50 signal)))]
    (println "   ⇒ kept" (count r) "values out of 6")))

;; ─── 5. amb / amb= ──────────────────────────────────────────────────────────

(defn ex-amb []
  (println "5) amb — sequential alternation; amb= — concurrent:")
  (println "   amb  ⇒" (m/? (m/reduce conj (m/ap (m/amb :a :b :c)))))
  (println "   amb= ⇒"
           (m/? (m/reduce conj
                  (m/ap (let [ms (m/amb= (m/? (m/sleep 100 :slow))
                                         (m/? (m/sleep 10 :fast)))]
                          ms))))))

;; ─── 6. Loop inside ap ───────────────────────────────────────────────────────

(defn ex-loop []
  (println "6) ap + loop — generate a flow imperatively:")
  (let [r (m/? (m/reduce conj
                 (m/ap (loop [[x & xs] (range 5)]
                         (if x (m/amb x (recur xs)) (m/amb))))))]
    (println "   ⇒" r)))

(defn -main [& _]
  (println "=== ch06 — ambiguous ===")
  (ex-basic-ap)
  (ex-fan-out)
  (ex-bounded)
  (ex-debounce)
  (ex-amb)
  (ex-loop)
  (shutdown-agents))
