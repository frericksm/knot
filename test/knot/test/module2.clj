(ns knot.test.module2
  "Module2" 
  {:knot-module true})


(def ^{:extension-point-ref "knot.test.module1.ep1" }
  extension2 
  "This is e2 from module 2")

(def ^{:extension-point-ref "knot.test.module1.ep1" }
  extension3 
  "This is e3 from module 2")

(def ^{:extension-point-ref "knot.test.module1.ep2" }
  extension4
  "This is e4 from module 2")
