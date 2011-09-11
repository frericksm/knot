(ns knot.test.module1
  "Module1" 
  {:knot-module true})

(def ^{:extension-point-id "knot.test.module1.ep1"}
  extension-point1
  "")

(def ^{:extension-point-id "knot.test.module1.ep2"}
  extension-point2  
  "")

(def ^{:extension-point-ref "knot.test.module1.ep1" }
  extension1
  "This is e1 from module 1")
