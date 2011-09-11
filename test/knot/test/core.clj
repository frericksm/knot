(ns knot.test.core
  "Test knot.core"
  (:use [knot.core] :reload)
  (:use [clojure.test])
  )


(deftest test-knot-module-namespaces
  (let [ens (knot-module-namespaces)]
    (is (= 2 (count ens)))
    (is (set (list 'knot.test.module1 'knot.test.module2)) (set (knot-module-namespaces)))
    ))


(deftest test-count-extension-and-points 
  (let [ex-p-m1 (extension-points 'knot.test.module1)
	ex-m1   (extensions       'knot.test.module1)
	ex-p-m2 (extension-points 'knot.test.module2)
	ex-m2   (extensions       'knot.test.module2)
	]
    (is (= 2 (count ex-p-m1)))
    (is (= 1 (count ex-m1)))
    (is (= 0 (count ex-p-m2)))    
    (is (= 3 (count ex-m2)))    
    ))

(deftest test-configuration-elements-for
  (let [conf-ep1 (configuration-elements-for "knot.test.module1.ep1")
	conf-ep2 (configuration-elements-for "knot.test.module1.ep2")
	]
    (is (= 3 (count conf-ep1)))
    (is (= 1 (count conf-ep2)))
    (is (= (set (list "This is e1 from module 1"
		      "This is e2 from module 2"
		      "This is e3 from module 2"))
	   (set conf-ep1)))
    (is (= (set (list "This is e4 from module 2"))
	   (set conf-ep2)))
    ))
