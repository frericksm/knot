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

(defn- ns-intern-with-meta-key
  "Returns the meta-data of vars defined in namespace ns only if the meta-data contains the key meta-key"
  [ns meta-key]
  (->> (ns-interns ns)
       (vals)
       (map meta)
       (filter #(contains? % meta-key))
       )
  )

(defn extension-points
  "Returns the meta-data of vars defined in namespace ns only if the meta-data contains the key :extension-point-id"
  [ns]
  (ns-intern-with-meta-key ns :extension-point-id))

(defn extensions
  "Returns the meta-data of vars defined in namespace ns only if the meta-data contains the key :extension-point-ref"
  [ns]
  (ns-intern-with-meta-key ns :extension-point-ref))

(defn build-registry
  "Builds the registry for the namespaces in seq knot-modules. The registry is map. It maps the extension-point-id to a seq of the meta-data of the extensions to that extension-point-id. Calls require for the namespaces in knot-modules in order to have access to the vars. Returns the registry."
  [knot-modules]
  (doseq [ns knot-modules]
    (require ns))
  (let [ext-point-ids     (->> knot-modules
			       (map extension-points)
			       (flatten)
			       (map :extension-point-id))
	extensions        (flatten (map extensions knot-modules))
	filter-extensions (fn [extension ext-point-id] (= ext-point-id (get extension :extension-point-ref)))]
    (reduce (fn [a ext-point-id] (assoc a ext-point-id (filter #(filter-extensions % ext-point-id) extensions)))	      
	    {}
	    ext-point-ids)))

(def *registry* (build-registry (knot-module-namespaces)))

(defn configuration-elements-for
  "Returns all extensions contributed to the extension-point-id"
  [extension-point-id]
  (->> (get *registry* extension-point-id)
       (map #(ns-resolve (get % :ns) (get % :name)))
       (map deref)))
