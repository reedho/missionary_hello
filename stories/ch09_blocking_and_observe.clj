(ns stories.ch09-blocking-and-observe
  "09 — Blocking I/O with m/via, bridging callback APIs with m/observe.

   Run with:  clj -M:09"
  (:require [missionary.core :as m]))

;; ─── 1. Blocking on the calling thread serializes ────────────────────────────

(defn ex-blocking-serial []
  (println "1) two Thread/sleep(200) under plain sp — serial on calling thread:")
  (let [t0 (System/currentTimeMillis)]
    (m/? (m/join vector
           (m/sp (Thread/sleep 200))
           (m/sp (Thread/sleep 200))))
    (println "   took" (- (System/currentTimeMillis) t0) "ms (≈400)")))

;; ─── 2. m/via m/blk parallelizes blocking work ───────────────────────────────

(defn ex-via-blk []
  (println "2) the same with m/via m/blk — actually concurrent:")
  (let [t0 (System/currentTimeMillis)]
    (m/? (m/join vector
           (m/via m/blk (Thread/sleep 200))
           (m/via m/blk (Thread/sleep 200))))
    (println "   took" (- (System/currentTimeMillis) t0) "ms (≈200)")))

;; ─── 3. CPU-bound work on m/cpu (just to show the pool) ──────────────────────

(defn ex-via-cpu []
  (println "3) m/via m/cpu — for CPU-bound work:")
  (let [n   100000
        sum (m/? (m/via m/cpu (reduce + (range n))))]
    (println "   sum 0.." n "⇒" sum)))

;; ─── 4. m/observe — bridge a callback-style source ───────────────────────────
;; Pretend external library: pushes 5 values asynchronously, then a nil sentinel.

(defn external-source [emit!]
  (future
    (doseq [v [:a :b :c :d :e]]
      (Thread/sleep 10)
      (emit! v))
    (emit! nil))                  ;; sentinel = "stream ended"
  ;; cleanup thunk (not exercised in this happy-path demo)
  (fn []))

(defn ex-observe []
  (println "4) m/observe + take-while — finite stream from a callback API:")
  (let [flow (m/observe external-source)
        r    (m/? (m/reduce conj
                   (m/eduction (take-while some?) flow)))]
    (println "   ⇒" r)))

;; ─── 5. Concurrent fan-out of blocking work ──────────────────────────────────
;; The bread-and-butter pattern for parallel HTTP, JDBC, etc.

(defn ex-fanout []
  (println "5) ap + ?> ##Inf + via m/blk — concurrent fan-out of blocking work:")
  (let [t0    (System/currentTimeMillis)
        delays [200 100 150 50]
        r     (m/? (m/reduce conj
                     (m/ap (let [d (m/?> ##Inf (m/seed delays))]
                             (m/? (m/via m/blk
                                    (Thread/sleep d)
                                    [:done d]))))))]
    (println "   ⇒" r "  took" (- (System/currentTimeMillis) t0) "ms (≈200)")))

(defn -main [& _]
  (println "=== ch09 — blocking & observe ===")
  (ex-blocking-serial)
  (ex-via-blk)
  (ex-via-cpu)
  (ex-observe)
  (ex-fanout)
  (shutdown-agents))
