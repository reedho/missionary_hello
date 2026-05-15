(ns stories.ch05-flow-basics
  "05 — Flow basics: seed, reduce, eduction, reductions, zip, observe.

   Run with:  clj -M:05"
  (:require [missionary.core :as m]))

;; ─── 1. seed + reduce ────────────────────────────────────────────────────────

(defn ex-seed-reduce []
  (println "1) seed + reduce — flow → task:")
  (println "   sum 0..9 ⇒" (m/? (m/reduce + (m/seed (range 10))))))

;; ─── 2. eduction — transducers on flows ──────────────────────────────────────

(defn ex-eduction []
  (println "2) eduction — fused transducer chain, zero intermediate alloc:")
  (let [r (m/? (m/reduce conj
                 (m/eduction (filter odd?) (map #(* % %))
                             (m/seed (range 10)))))]
    (println "   odds², 0..9 ⇒" r)))

;; ─── 3. reductions — running aggregates ──────────────────────────────────────

(defn ex-reductions []
  (println "3) reductions — running aggregates as a flow:")
  (let [r (m/? (m/reduce conj
                 (m/reductions + 0 (m/seed [1 2 3 4]))))]
    (println "   cumulative sums ⇒" r)))

;; ─── 4. zip — element-wise combine ───────────────────────────────────────────

(defn ex-zip []
  (println "4) zip — element-wise:")
  (let [r (m/? (m/reduce conj
                 (m/zip vector
                        (m/seed [:a :b :c :d])
                        (m/seed [1 2 3]))))]
    (println "   ⇒" r)))

;; ─── 5. observe — bridge a callback API ──────────────────────────────────────
;; Simulate a callback source: a thread that pushes 3 values 30ms apart.

(defn fake-events
  "Like an event-emitter. Returns a fn to subscribe; subscriber gets values."
  []
  (let [subs (atom #{})]
    {:subscribe (fn [cb]
                  (swap! subs conj cb)
                  #(swap! subs disj cb))           ;; unsubscribe thunk
     :emit!     (fn [v] (doseq [cb @subs] (cb v)))}))

(defn ex-observe []
  (println "5) observe — bridge a push-based source into a flow:")
  (let [{:keys [subscribe emit!]} (fake-events)
        events (m/observe
                 (fn [emit-into-flow!]
                   (let [unsub! (subscribe emit-into-flow!)]
                     ;; cleanup thunk:
                     unsub!)))
        ;; Drive the source from another thread
        _ (future (Thread/sleep 20) (emit! :click-1)
                  (Thread/sleep 20) (emit! :click-2)
                  (Thread/sleep 20) (emit! :click-3))
        ;; Take only the first 3 events, then the flow terminates and cancels the subject.
        r (m/? (m/reduce conj (m/eduction (take 3) events)))]
    (println "   first 3 events ⇒" r)))

(defn -main [& _]
  (println "=== ch05 — flow basics ===")
  (ex-seed-reduce)
  (ex-eduction)
  (ex-reductions)
  (ex-zip)
  (ex-observe)
  (shutdown-agents))
