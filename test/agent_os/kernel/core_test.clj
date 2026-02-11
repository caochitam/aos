(ns agent-os.kernel.core-test
  (:require [clojure.test :refer :all]
            [agent-os.kernel.core :as kernel]
            [agent-os.kernel.protocols :refer [boot shutdown status
                                               get-component list-components
                                               register-component
                                               component-id component-version]]))

(deftest kernel-lifecycle-test
  (testing "Kernel boot and shutdown lifecycle"
    (let [k (kernel/create-kernel {:test true})]

      (testing "Initial kernel state"
        (is (not (nil? k)))
        (is (= :created (get-in @(:state k) [:status]))))

      (testing "Kernel boots successfully"
        (let [booted (boot k {:test true})]
          (is (= :running (get-in @(:state booted) [:status])))
          (is (number? (get-in @(:state booted) [:boot-time])))))

      (testing "Kernel shuts down"
        (boot k {:test true})
        (let [result (shutdown k)]
          (is (nil? result))
          (is (= :shutdown (get-in @(:state k) [:status]))))))))

(deftest component-registration-test
  (testing "Component registration"
    (let [k (boot (kernel/create-kernel {}) {})
          comp (kernel/create-component
                 :test/example-component
                 '(defn example-fn [x] (* x 2))
                 :purpose "Test component"
                 :interfaces [:compute]
                 :dependencies #{})]

      (testing "Register a component"
        (register-component k comp)
        (is (= [:test/example-component] (list-components k))))

      (testing "Retrieve registered component"
        (let [retrieved (get-component k :test/example-component)]
          (is (not (nil? retrieved)))
          (is (= :test/example-component (component-id retrieved)))
          (is (= 1 (component-version retrieved)))))

      (testing "Cannot register kernel namespace component"
        (let [kernel-comp (kernel/create-component
                            :agent-os.kernel/protected
                            '(defn bad [])
                            :purpose "Bad component")]
          (is (thrown? Exception
                       (register-component k kernel-comp))))))))

(deftest component-creation-test
  (testing "Create component with defaults"
    (let [comp (kernel/create-component
                 :test/simple
                 '(defn simple [])
                 :purpose "Simple test")]
      (is (= :test/simple (component-id comp)))
      (is (= 1 (component-version comp)))
      (is (true? (:modifiable? comp)))
      (is (number? (:created-at comp)))))

  (testing "Create immutable component"
    (let [comp (kernel/create-component
                 :test/immutable
                 '(defn immutable [])
                 :purpose "Immutable"
                 :modifiable? false)]
      (is (false? (:modifiable? comp))))))

(deftest kernel-status-test
  (testing "Kernel status reporting"
    (let [k (boot (kernel/create-kernel {}) {})]
      (let [s (status k)]
        (is (= :running (:status s)))
        (is (= kernel/KERNEL_VERSION (:version s)))
        (is (number? (:uptime s)))
        (is (= 0 (:total-components s)))))))

(deftest system-state-test
  (testing "Get system state snapshot"
    (let [k (boot (kernel/create-kernel {}) {})
          comp (kernel/create-component
                 :test/comp1
                 '(defn f [])
                 :purpose "Test")]
      (register-component k comp)
      (let [state (kernel/get-system-state k)]
        (is (= :running (:status state)))
        (is (= 1 (:total-components state)))
        (is (= 1 (:modifiable-components state)))
        (is (= [:test/comp1] (:component-ids state)))))))

(deftest kernel-health-test
  (testing "Kernel health check"
    (let [k (boot (kernel/create-kernel {}) {})]
      (is (true? (kernel/kernel-healthy? k)))
      (shutdown k)
      (is (false? (kernel/kernel-healthy? k))))))
