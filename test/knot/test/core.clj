(ns knot.test.core
  "Test knot.core"
  (:use [knot.core])
  (:use [clojure.test])
  )


(deftest test-knot-module-namespaces
  (let [r (build-registry)]
    (is (= 2 (count (contributions r :knot.test.module1/ep1)))
    )))


