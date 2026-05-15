(ns stories.ch10-testing-test
  (:require [clojure.test :refer [deftest is testing]]
            [missionary.core :as m]))

(deftest sleep-returns-its-value
  (testing "m/sleep with a value resolves to that value"
    (is (= :ok (m/? (m/sleep 5 :ok))))
    (is (nil? (m/? (m/sleep 5))))))

(deftest sp-throws-rethrow-through-bang
  (testing "failure in sp surfaces as a regular throw at the m/? call site"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"boom"
          (m/? (m/sp (throw (ex-info "boom" {}))))))))

(deftest attempt-wraps-into-a-thunk
  (testing "attempt always succeeds; thunk replays the original outcome"
    (let [thunk (m/? (m/attempt (m/sp :ok)))]
      (is (= :ok (thunk))))
    (let [thunk (m/? (m/attempt (m/sp (throw (ex-info "boom" {})))))]
      (is (thrown? clojure.lang.ExceptionInfo (thunk))))))

(deftest race-picks-first-success
  (testing "race returns the first successful result, cancels the rest"
    (is (= :fast (m/? (m/race (m/sleep 100 :slow)
                              (m/sleep 10 :fast)))))))

(deftest seed-and-reduce-roundtrip
  (testing "reducing a seed flow"
    (is (= 45 (m/? (m/reduce + (m/seed (range 10))))))))

(deftest eduction-applies-transducers
  (testing "transducer fusion via eduction"
    (is (= [0 1 4 9 16]
           (m/? (m/reduce conj
                  (m/eduction (map #(* % %)) (m/seed (range 5)))))))))

(deftest timeout-cancels-and-falls-back
  (testing "timeout returns the fallback value once the deadline passes"
    (is (= :late (m/? (m/timeout (m/sleep 100 :a) 10 :late))))))

(deftest signal-propagation
  (testing "atom watch + signal + take produces ordered states"
    (let [!v (atom 0)
          <v (m/signal (m/watch !v))
          fut (future (m/? (m/reduce conj (m/eduction (take 3) <v))))]
      (Thread/sleep 30) (swap! !v inc)
      (Thread/sleep 30) (swap! !v inc)
      (is (= [0 1 2] @fut)))))
