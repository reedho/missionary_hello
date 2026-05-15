(ns stories.ch07-signals
  "07 — Signals: cp, watch, signal, latest. Glitch-free reactive DAG.

   Run with:  clj -M:07"
  (:require [missionary.core :as m]))

;; ─── 1. watch — an atom as a continuous flow ─────────────────────────────────

(defn ex-watch []
  (println "1) watch — observe atom changes (we take 3 then stop):")
  (let [!count (atom 0)
        >count (m/watch !count)
        ;; Run in another thread so we can mutate the atom from main.
        result (future
                 (m/? (m/reduce conj (m/eduction (take 3) >count))))]
    (Thread/sleep 30) (swap! !count inc)
    (Thread/sleep 30) (swap! !count inc)
    (println "   ⇒" @result)))

;; ─── 2. signal — multicast a flow (replay-1) ─────────────────────────────────

(defn ex-signal []
  (println "2) signal — shared continuous flow, replay-1 for late subscribers:")
  (let [!v   (atom 100)
        <v   (m/signal (m/watch !v))
        f1   (future (m/? (m/reduce conj (m/eduction (take 2) <v))))
        _    (Thread/sleep 30)
        ;; late subscriber — sees current value immediately
        f2   (future (m/? (m/reduce conj (m/eduction (take 1) <v))))]
    (Thread/sleep 30) (swap! !v inc)
    (println "   early ⇒" @f1 "   late ⇒" @f2)))

;; ─── 3. latest — derived signal (sum of two upstreams) ───────────────────────

(defn ex-latest []
  (println "3) latest — derived signal recomputes on any upstream change:")
  (let [!x  (atom 1) !y (atom 10)
        <x  (m/signal (m/watch !x))
        <y  (m/signal (m/watch !y))
        <s  (m/signal (m/latest + <x <y))
        out (atom [])
        cancel! ((m/reduce (fn [_ v] (swap! out conj v)) nil <s)
                 (constantly nil) (constantly nil))]
    (Thread/sleep 30)
    (swap! !x inc)         ;; 11 -> 12
    (Thread/sleep 30)
    (swap! !y inc)         ;; 12 -> 13
    (Thread/sleep 30)
    (cancel!)
    (println "   propagation trace ⇒" @out)))

;; ─── 4. Glitch-free diamond DAG ──────────────────────────────────────────────

(defn ex-diamond []
  (println "4) glitch-free diamond — <y depends on <x twice; never sees inconsistent state:")
  (let [!input (atom 1)
        <x     (m/signal (m/watch !input))
        <y     (m/signal (m/latest + <x <x))
        out    (atom [])
        cancel! ((m/reduce (fn [_ v] (swap! out conj v)) nil <y)
                 (constantly nil) (constantly nil))]
    (Thread/sleep 30)
    (swap! !input inc)            ;; 1 -> 2 ; <y should jump 2 -> 4, no '3'
    (Thread/sleep 30)
    (swap! !input inc)            ;; 2 -> 3 ; <y should jump 4 -> 6, no '5'
    (Thread/sleep 30)
    (cancel!)
    (println "   <y trace ⇒" @out "  (note: no 3 or 5)")))

;; ─── 5. cp — continuous process body ─────────────────────────────────────────

(defn ex-cp []
  (println "5) cp — build a signal with a body that samples another signal:")
  (let [!v       (atom 2)
        <v       (m/signal (m/watch !v))
        <squared (m/signal (m/cp (let [x (m/?< <v)] (* x x))))
        out      (atom [])
        cancel!  ((m/reduce (fn [_ v] (swap! out conj v)) nil <squared)
                  (constantly nil) (constantly nil))]
    (Thread/sleep 30)
    (swap! !v inc) (Thread/sleep 30)
    (swap! !v inc) (Thread/sleep 30)
    (cancel!)
    (println "   squares ⇒" @out)))

(defn -main [& _]
  (println "=== ch07 — signals ===")
  (ex-watch)
  (ex-signal)
  (ex-latest)
  (ex-diamond)
  (ex-cp)
  (shutdown-agents))
