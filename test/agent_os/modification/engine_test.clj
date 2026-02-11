(ns agent-os.modification.engine-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [agent-os.modification.engine :as mod]
            [agent-os.kernel.core :as kernel]
            [agent-os.kernel.protocols :refer [boot register-component]]))

(deftest create-proposal-test
  (testing "Create modification proposal"
    (let [old-code '(defn old [x] x)
          new-code '(defn new [x] (* x 2))
          proposal (mod/create-proposal
                     :test/comp
                     old-code
                     new-code
                     "Improve performance"
                     {:type :optimization})]
      (is (= :test/comp (:component-id proposal)))
      (is (= old-code (:old-code proposal)))
      (is (= new-code (:new-code proposal)))
      (is (= :proposed (:status proposal)))
      (is (string? (:id proposal))))))

(deftest validate-syntax-test
  (testing "Valid Clojure syntax"
    (let [result (mod/validate-syntax '(defn add [x y] (+ x y)))]
      (is (:valid? result))
      (is (sequential? (:forms result)))))

  (testing "Invalid syntax"
    (let [result (mod/validate-syntax "(defn broken [x")]
      (is (not (:valid? result)))
      (is (string? (:error result))))))

(deftest validate-size-test
  (testing "Code within size limit"
    (let [code '(defn small [] 42)
          result (mod/validate-size code 1000)]
      (is (:valid? result))
      (is (pos? (:size result)))))

  (testing "Code exceeds size limit"
    (let [code (str/join (repeat 1000 "(defn huge [] 42)"))
          result (mod/validate-size code 100)]
      (is (not (:valid? result)))
      (is (string? (:error result))))))

(deftest validate-not-kernel-test
  (testing "Non-kernel component allowed"
    (let [result (mod/validate-not-kernel :user/component ["agent-os.kernel"])]
      (is (:valid? result))))

  (testing "Kernel component blocked"
    (let [result (mod/validate-not-kernel :agent-os.kernel/core ["agent-os.kernel"])]
      (is (not (:valid? result)))
      (is (string? (:error result))))))

(deftest validate-proposal-test
  (testing "Valid proposal passes all checks"
    (let [proposal (mod/create-proposal
                     :test/valid
                     '(defn old [])
                     '(defn new [] 42)
                     "Test"
                     {})
          config {:safety {:max-code-size-bytes 10000
                           :protected-namespaces ["agent-os.kernel"]}}
          result (mod/validate-proposal proposal config {})]
      (is (:valid? result))
      (is (empty? (:errors result)))))

  (testing "Invalid proposal fails checks"
    (let [proposal (mod/create-proposal
                     :agent-os.kernel/protected
                     '(defn old [])
                     '(defn new [])
                     "Bad"
                     {})
          config {:safety {:max-code-size-bytes 10000
                           :protected-namespaces ["agent-os.kernel"]}}
          result (mod/validate-proposal proposal config {})]
      (is (not (:valid? result)))
      (is (seq (:errors result))))))

(deftest apply-modification-test
  (testing "Apply modification to existing component"
    (let [k (boot (kernel/create-kernel {}) {})
          comp (kernel/create-component
                 :test/modify
                 '(defn original [x] x)
                 :purpose "Test")
          _ (register-component k comp)
          proposal (mod/create-proposal
                     :test/modify
                     '(defn original [x] x)
                     '(defn modified [x] (* x 2))
                     "Improve"
                     {})
          result (mod/apply-modification k proposal)]
      (is (:success? result))
      (is (= 2 (get-in result [:component :version])))
      (is (= '(defn modified [x] (* x 2))
             (get-in result [:component :code]))))))

(deftest rollback-modification-test
  (testing "Rollback to previous version"
    (let [k (boot (kernel/create-kernel {}) {})
          comp (kernel/create-component
                 :test/rollback
                 '(defn v1 [])
                 :purpose "Test")
          _ (register-component k comp)
          proposal (mod/create-proposal
                     :test/rollback
                     '(defn v1 [])
                     '(defn v2 [])
                     "Update"
                     {})
          _ (mod/apply-modification k proposal)
          rollback-result (mod/rollback-modification k :test/rollback)]
      (is (:success? rollback-result))
      (is (= '(defn v1 [])
             (get-in rollback-result [:component :code]))))))

(deftest modification-history-test
  (testing "Record and retrieve modifications"
    (let [history (mod/create-history)
          proposal (mod/create-proposal
                     :test/hist
                     '(defn old [])
                     '(defn new [])
                     "Test"
                     {})
          result {:success? true}]

      (mod/record-modification history proposal result)

      (is (= 1 (count (:modifications @history))))
      (is (= 1 (count (:successful @history))))
      (is (= 0 (count (:failed @history))))

      (let [stats (mod/modification-stats history)]
        (is (= 1 (:total stats)))
        (is (= 1 (:successful stats)))
        (is (= 1.0 (:success-rate stats)))))))
