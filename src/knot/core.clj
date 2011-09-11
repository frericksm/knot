(ns knot.core
  "An extension registry."
  (:require [clojure.contrib.find-namespaces :as fn])
)

(defn knot-module-namespaces
  "Searches CLASSPATH (both directories and JAR files) for Clojure source
files containing (ns ...) declarations with meta-data :knot-module set to true.
Returns a sequence of the symbol names of the declared namespaces."
  []
  (->> (fn/find-ns-decls-on-classpath)
       (filter #(some (fn [m] (get m :knot-module))
		      (filter map? %)))
       (map second)))

(defn ns-intern-with-meta-key [ns meta-key]
  (->> (ns-interns ns)
       (vals)
       (map meta)
       (filter #(contains? % meta-key))
       )
  )


(defn extension-points [ns]
  (ns-intern-with-meta-key ns :extension-point-id))

(defn extensions [ns]
  (ns-intern-with-meta-key ns :extension-point-ref))

(defn build-registry
  ""
  [knot-modules]
  (doseq [ns knot-modules]
    (require ns))
  (let [ext-points (flatten (map extension-points
				 knot-modules))
	ext        (flatten (map extensions
				 knot-modules))
	filter-extensions (fn [extension ext-point-id]
			    (= ext-point-id
			       (get extension :extension-point-ref)))]
    (reduce (fn [a ep]
	      (let [ext-point-id (get ep :extension-point-id)]
		(assoc a ext-point-id
		       (filter #(filter-extensions % ext-point-id) ext)
		       )
		)
	      )
	    {}
	    ext-points)))
    

(def *registry* (build-registry (knot-module-namespaces)))
  

(defn configuration-elements-for [extension-point-id]
  (->> (get *registry* extension-point-id)
       (map #(ns-resolve (get % :ns) (get % :name)))
       (map deref)))
