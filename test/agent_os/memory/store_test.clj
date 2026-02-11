(ns agent-os.memory.store-test
  (:require [clojure.test :refer :all]
            [agent-os.memory.store :as mem]
            [clojure.java.io :as io]))

(def test-config
  {:memory {:base-path "target/test-data/"
            :memory-file "MEMORY.edn"
            :daily-log-dir "memory/"
            :history-dir "history/"}})

(deftest create-memory-system-test
  (testing "Create memory system"
    (let [msys (mem/create-memory-system test-config)]
      (is (not (nil? msys)))
      (is (= "target/test-data/MEMORY.edn" (:memory-file msys))))))

(deftest remember-fact-test
  (testing "Remember and recall facts"
    ;; Clean up first
    (.delete (io/file "target/test-data/MEMORY.edn"))
    (let [msys (mem/create-memory-system test-config)
          fact (mem/remember-fact msys :test "Test fact" :test-source 0.9)
          memory (mem/load-memory msys)]
      (is (= 1 (count (:facts memory))))
      (is (= "Test fact" (-> memory :facts first :content))))))

(deftest remember-decision-test
  (testing "Remember decisions"
    ;; Clean up first
    (.delete (io/file "target/test-data/MEMORY.edn"))
    (let [msys (mem/create-memory-system test-config)
          decision (mem/remember-decision msys "Use EDN" "Native Clojure format")
          memory (mem/load-memory msys)]
      (is (= 1 (count (:decisions memory))))
      (is (= "Use EDN" (-> memory :decisions first :decision))))))

(deftest daily-log-test
  (testing "Append to daily log"
    (let [msys (mem/create-memory-system test-config)
          entry (mem/append-daily-log msys {:type :test :message "Test entry"})]
      (is (not (nil? (:timestamp entry)))))))

(deftest context-compaction-test
  (testing "Estimate context size"
    (let [messages ["Hello" "World" "Test"]
          size (mem/estimate-context-size messages)]
      (is (pos? size))))

  (testing "Should compact decision"
    (let [messages (repeat 10000 "message")
          config {:memory {:max-context-tokens 1000 :flush-threshold 0.8}}]
      (is (true? (mem/should-compact? messages config))))))
