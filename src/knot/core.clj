(ns knot.core 
  (:require [clojure.contrib.find-namespaces :as fn])
)

(defn extension-ns
  "Searches CLASSPATH (both directories and JAR files) for Clojure
source files containing (ns ...{:extreg true}) declarations.
Returns a sequence of the symbol names of the declared namespaces."
  []
  (->> (fn/find-ns-decls-on-classpath)
       (filter #(some (fn [m] (get m :extreg))
		      (filter map? %)))
       (map second)))

(defn extension-points []
  (->> (ns-interns 'tools.test.extreg)
       (vals)
       (map meta)
       (filter #(= :extension-point (get % :extreg-type)))
       ))

(defn extensions []
  (->> (ns-interns 'tools.test.extreg)
       (vals)
       (map meta)
       (filter #(= :extension (get % :extreg-type)))
       ))

(defn configuration-elements-for [extension-point]
  (doseq [ns (->> (fn/find-ns-decls-on-classpath)
		  (filter #(some (fn [m] (get m :extreg))
				 (filter map? %))))]
    (require-may-fail ns)))
  
