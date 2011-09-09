(ns tools.test.extreg
  "Test der extreg"
  {:extreg true}
  (:use [tools.extreg] :reload)
  (:use [clojure.test])
  )


(defn ep1 "" {:extreg-type :extension-point } []
  {:id "tools.test.extreg.ep1" })

(defn e1 "" {:extreg-type :extension :extension-point "tools.test.extreg.ep1" }
  []
  "e1 was called")

(deftest test-extension-ns 
  (let [ens (extension-ns)]
    (is (= 1 (count ens)))
    ))


(deftest test-extension-points 
  (let [ens (extension-points)]
    (is (= 1 (count ens)))
    ))
