(ns stories.ch04-communication
  "04 — Communication: dfv, mbx, rdv, sem, holding.

   Run with:  clj -M:04"
  (:require [missionary.core :as m]))

;; ─── 1. dfv — dataflow variable (one-shot promise) ───────────────────────────

(defn ex-dfv []
  (println "1) dfv: assign once, await with m/?:")
  (let [d (m/dfv)]
    (future (Thread/sleep 50) (d 42))
    (println "   ⇒" (m/? d))))

(defn ex-dfv-request-reply []
  (println "2) dfv as a reply channel for request/response:")
  (let [reply (m/dfv)
        worker (fn [req reply]
                 (future (Thread/sleep 30) (reply (str "echo " req))))]
    (worker "ping" reply)
    (println "   ⇒" (m/? reply))))

;; ─── 2. mbx — mailbox + actor ────────────────────────────────────────────────

(defn ex-mailbox []
  (println "3) mbx: post! + await arrival order:")
  (let [post! (m/mbx)]
    (post! :hello)
    (post! :world)
    (println "   pull 1 ⇒" (m/? post!))
    (println "   pull 2 ⇒" (m/? post!))))

(defn actor
  "Tail-loop over a behavior. Each behavior call gets (self, message) and
   returns the next behavior."
  [behavior]
  (let [self (m/mbx)]
    ((m/sp (loop [b behavior]
             (recur (b self (m/? self)))))
     (constantly nil) (constantly nil))
    self))

(defn ex-counter-actor []
  (println "4) actor with behavior (counter):")
  (let [counter (actor
                  ((fn beh [n]
                     (fn [_self [op reply]]
                       (case op
                         :get  (do (reply n) (beh n))
                         :inc  (beh (inc n)))))
                   0))]
    (let [r (m/dfv)] (counter [:get r]) (println "   get ⇒" (m/? r)))
    (counter [:inc nil])
    (counter [:inc nil])
    (let [r (m/dfv)] (counter [:get r]) (println "   get ⇒" (m/? r)))))

;; ─── 3. rdv — rendezvous (synchronous handoff) ───────────────────────────────

(defn ex-rdv []
  (println "5) rdv: give parks until taken (synchronous):")
  (let [r (m/rdv)]
    (future
      (Thread/sleep 30)
      (m/? (r :handoff)))      ;; give parks until somebody takes
    (println "   take ⇒" (m/? r))))

;; ─── 4. sem + holding ────────────────────────────────────────────────────────

(defn ex-sem []
  (println "6) sem of 2 — three workers, third waits one slot:")
  (let [permit (m/sem 2)
        t0     (System/currentTimeMillis)
        worker (fn [tag]
                 (m/sp
                   (m/holding permit
                     (m/? (m/sleep 100 tag)))))]
    (let [r (m/? (m/join vector (worker :a) (worker :b) (worker :c)))]
      (println "   ⇒" r
               "  took" (- (System/currentTimeMillis) t0) "ms (≈200)"))))

(defn -main [& _]
  (println "=== ch04 — communication ===")
  (ex-dfv)
  (ex-dfv-request-reply)
  (ex-mailbox)
  (ex-counter-actor)
  (ex-rdv)
  (ex-sem)
  (shutdown-agents))
