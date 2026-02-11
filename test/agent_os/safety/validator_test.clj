(ns agent-os.safety.validator-test
  (:require [clojure.test :refer :all]
            [agent-os.safety.validator :as safe]))

(def test-config
  {:safety {:protected-namespaces ["agent-os.kernel"]
            :max-code-size-bytes 10000
            :allowed-requires #{"clojure.string" "clojure.set"}}})

(deftest validate-syntax-test
  (testing "Valid syntax"
    (let [result (safe/validate-syntax '(defn add [x y] (+ x y)))]
      (is (:valid? result))))

  (testing "Invalid syntax"
    (let [result (safe/validate-syntax "(defn broken [x")]
      (is (not (:valid? result))))))

(deftest check-dangerous-symbols-test
  (testing "Safe code passes"
    (let [result (safe/check-dangerous-symbols '(defn safe [x] (+ x 1)))]
      (is (:valid? result))))

  (testing "Eval is dangerous"
    (let [result (safe/check-dangerous-symbols '(defn bad [] (eval '(+ 1 2))))]
      (is (not (:valid? result))))))

(deftest check-infinite-loops-test
  (testing "Normal loop is OK"
    (let [result (safe/check-infinite-loops '(loop [x 10] (when (pos? x) (recur (dec x)))))]
      (is (:valid? result))))

  (testing "While true detected"
    (let [result (safe/check-infinite-loops '(while true (println "bad")))]
      (is (not (:valid? result))))))

(deftest check-protected-namespace-test
  (testing "User namespace allowed"
    (let [result (safe/check-protected-namespace :user/component ["agent-os.kernel"])]
      (is (:valid? result))))

  (testing "Kernel namespace blocked"
    (let [result (safe/check-protected-namespace :agent-os.kernel/core ["agent-os.kernel"])]
      (is (not (:valid? result))))))

(deftest validate-code-test
  (testing "Valid code passes all checks"
    (let [code '(defn simple [x] (* x 2))
          result (safe/validate-code code test-config :component-id :user/test)]
      (is (:valid? result))
      (is (empty? (:errors result)))))

  (testing "Kernel modification blocked"
    (let [code '(defn bad [])
          result (safe/validate-code code test-config :component-id :agent-os.kernel/test)]
      (is (not (:valid? result))))))
