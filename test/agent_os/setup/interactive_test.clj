(ns agent-os.setup.interactive-test
  "Tests for interactive API key setup"
  (:require [clojure.test :refer :all]
            [agent-os.setup.interactive :as sut]))

(deftest test-valid-api-key
  (testing "Valid API key formats"
    (is (sut/valid-api-key? "sk-ant-api03-1234567890abcdefghij"))
    (is (sut/valid-api-key? "sk-ant-1234567890abcdefghij1234567890"))
    (is (sut/valid-api-key? "sk-ant-api03-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")))

  (testing "Invalid API key formats"
    (is (not (sut/valid-api-key? "")))
    (is (not (sut/valid-api-key? nil)))
    (is (not (sut/valid-api-key? "   ")))
    (is (not (sut/valid-api-key? "sk-ant")))
    (is (not (sut/valid-api-key? "sk-ant-")))
    (is (not (sut/valid-api-key? "sk-ant-short")))
    (is (not (sut/valid-api-key? "invalid-key-format")))
    (is (not (sut/valid-api-key? "apikey123456789012345678")))))

(deftest test-get-shell-rc-file
  (testing "Detects correct shell RC file"
    (let [rc-file (sut/get-shell-rc-file)]
      (is (string? rc-file))
      (is (or (.endsWith rc-file ".bashrc")
              (.endsWith rc-file ".zshrc"))))))

(deftest test-check-api-key
  (testing "Check existing API key from environment"
    ;; Note: Environment variables take precedence over system properties
    ;; We can only test what's currently set
    (let [result (sut/check-api-key)]
      (is (or (string? result) (nil? result))))))

(deftest test-get-home-dir
  (testing "Get home directory"
    (let [home (sut/get-home-dir)]
      (is (string? home))
      (is (not (empty? home)))
      (is (.startsWith home "/")))))

;; Integration-style tests (don't actually write files)

(deftest test-setup-current-session-only
  (testing "Setup current session only"
    (let [result (sut/setup-current-session-only "sk-ant-test-key-12345678901234567890")]
      (is (true? result))
      ;; Verify property was set
      (is (= "sk-ant-test-key-12345678901234567890"
             (System/getProperty "ANTHROPIC_API_KEY")))

      ;; Cleanup
      (System/clearProperty "ANTHROPIC_API_KEY"))))

;; Mock tests for file operations (to avoid actually modifying files)

(comment
  ;; These tests would modify actual files, so they're in a comment block
  ;; Run manually in a test environment if needed

  (deftest test-setup-bashrc-method
    (testing "Setup bashrc method"
      (let [result (sut/setup-bashrc-method "sk-ant-test-key")]
        (is (or (true? result) (false? result))))))

  (deftest test-setup-secure-file-method
    (testing "Setup secure file method"
      (let [result (sut/setup-secure-file-method "sk-ant-test-key")]
        (is (or (true? result) (false? result)))))))

;; ============================================================================
;; VALIDATION TESTS
;; ============================================================================

(deftest test-api-key-format-variations
  (testing "Real Anthropic API key format"
    ;; Real format: sk-ant-api03-[95 chars]
    (let [valid-key (str "sk-ant-api03-" (apply str (repeat 95 \x)))]
      (is (sut/valid-api-key? valid-key))))

  (testing "Alternative format: sk-ant-[chars]"
    (let [valid-key (str "sk-ant-" (apply str (repeat 40 \x)))]
      (is (sut/valid-api-key? valid-key))))

  (testing "Edge cases"
    ;; Exactly 21 chars (minimum valid)
    (is (sut/valid-api-key? "sk-ant-12345678901234"))

    ;; 20 chars (too short)
    (is (not (sut/valid-api-key? "sk-ant-1234567890123")))

    ;; Contains spaces (invalid)
    (is (not (sut/valid-api-key? "sk-ant-123 456 789")))

    ;; Wrong prefix
    (is (not (sut/valid-api-key? "sk-api-12345678901234567890")))))

(deftest test-ensure-api-key-configured
  (testing "When API key already exists"
    ;; If environment has a key, should return it without prompting
    (let [result (sut/ensure-api-key-configured)]
      ;; Should return a string (either from env or after setup)
      (is (or (string? result) (nil? result))))))

;; ============================================================================
;; RUN TESTS
;; ============================================================================

(comment
  ;; Run all tests
  (run-tests)

  ;; Run specific test
  (test-valid-api-key)
  (test-check-api-key))
