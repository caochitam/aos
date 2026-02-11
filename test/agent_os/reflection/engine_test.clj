(ns agent-os.reflection.engine-test
  (:require [clojure.test :refer :all]
            [agent-os.reflection.engine :as reflect]
            [agent-os.kernel.core :as kernel]))

(deftest namespace-to-filepath-test
  (testing "Convert namespace to file path"
    (is (= "agent_os/kernel/core.clj"
           (reflect/namespace->file-path 'agent-os.kernel.core)))
    (is (= "clojure/string.clj"
           (reflect/namespace->file-path 'clojure.string)))))

(deftest parse-source-test
  (testing "Parse Clojure source code"
    (let [source "(ns test.ns)\n(defn add [x y] (+ x y))"
          parsed (reflect/parse-source source)]
      (is (sequential? parsed))
      (is (= 2 (count parsed)))
      (is (= 'ns (first (first parsed))))
      (is (= 'defn (first (second parsed)))))))

(deftest extract-functions-test
  (testing "Extract function definitions from forms"
    (let [forms '[(ns test.ns)
                  (def x 42)
                  (defn add [x y] (+ x y))
                  (defn sub [x y] (- x y))]
          functions (reflect/extract-functions forms)]
      (is (= 2 (count functions)))
      (is (every? #(= 'defn (first %)) functions)))))

(deftest function-signature-test
  (testing "Extract function signature"
    (let [defn-form '(defn add "Adds two numbers" [x y] (+ x y))
          sig (reflect/function-signature defn-form)]
      (is (= 'add (:name sig)))
      (is (= '[x y] (:params sig)))
      (is (= "Adds two numbers" (:docstring sig)))))

  (testing "Function without docstring"
    (let [defn-form '(defn multiply [x y] (* x y))
          sig (reflect/function-signature defn-form)]
      (is (= 'multiply (:name sig)))
      (is (= '[x y] (:params sig)))
      (is (nil? (:docstring sig))))))

(deftest count-lines-test
  (testing "Count non-blank, non-comment lines"
    (let [source ";; Comment\n(defn f []\n  ; inline comment\n  42)\n\n"]
      (is (= 2 (reflect/count-lines source))))))

(deftest nesting-depth-test
  (testing "Calculate nesting depth"
    (is (= 0 (reflect/nesting-depth 42)))
    (is (= 1 (reflect/nesting-depth '(+ 1 2))))
    (is (= 2 (reflect/nesting-depth '(if true (+ 1 2)))))
    (is (= 3 (reflect/nesting-depth '(if true (if false (+ 1 2))))))))

(deftest analyze-component-test
  (testing "Analyze component structure"
    (let [comp (kernel/create-component
                 :test/analyzer
                 '(defn process [x] (if (> x 0) (* x 2) 0))
                 :purpose "Test analyzer"
                 :interfaces [:compute])
          analysis (reflect/analyze-component comp)]
      (is (= :test/analyzer (:id analysis)))
      (is (= 1 (:version analysis)))
      (is (true? (:modifiable? analysis)))
      (is (pos? (:code-size analysis)))
      (is (= 1 (:function-count analysis)))
      (is (= 'process (-> analysis :function-signatures first :name))))))

(deftest list-dependencies-test
  (testing "List component dependencies"
    (let [source "(ns test.ns (:require [clojure.string :as str] [clojure.set]))"
          parsed (reflect/parse-source source)
          requires (reflect/extract-requires parsed)]
      (is (= 2 (count requires)))
      (is (some #(= 'clojure.string %) requires)))))

(deftest find-issues-test
  (testing "Detect code smells"
    (let [deep-nested '(defn deep [x]
                         (if x
                           (if x
                             (if x
                               (if x
                                 (if x
                                   (if x 1)))))))
          comp (kernel/create-component
                 :test/bad
                 deep-nested
                 :purpose "Bad code")
          issues (reflect/find-issues comp)]
      (is (seq issues))
      (is (some #(= :deep-nesting (:type %)) issues)))))

(deftest component-complexity-test
  (testing "Calculate component complexity"
    (let [comp (kernel/create-component
                 :test/complex
                 '(defn calc [x]
                   (if (> x 0)
                     (* x 2)
                     0))
                 :purpose "Test")
          complexity (reflect/component-complexity comp)]
      (is (number? (:complexity-score complexity)))
      (is (pos? (:lines-of-code complexity)))
      (is (= 1 (:function-count complexity))))))
