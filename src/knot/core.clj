(ns knot.core
  "An extension registry."
  (:require [clojure.spec :as s]
            [clojure.java.io :as io]
            [clojure.java.classpath :as cjc]
            [clojure.tools.namespace.find :as ctnf]
            [clojure.tools.logging :as log])
  (:import (java.io File FileReader BufferedReader PushbackReader
                    InputStreamReader)
           (java.util.jar JarFile JarEntry)))

(defonce ^:private registry-ref (atom {}))

(defn knot-file?
  "Returns true if the java.io.File represents a knot file which is a file containing
a map serialized as an edn data structure."
  [^java.io.File file]
  (and (.isFile file)
       (= "knot.edn" (.getName file))))

(defn- jar-files
  "Given a sequence of File objects, filters it for JAR files, returns
  a sequence of java.util.jar.JarFile objects."
  [files]
  (map #(JarFile. ^File %) (filter cjc/jar-file? files)))
 
(defn knot-files-in-jar
  "Returns a sequence of filenames ending in .clj or .cljc found in the JAR file."
  [^JarFile jar-file]
  (filter #(.endsWith ^String % ".knt")
          (cjc/filenames-in-jar jar-file)))

(defn read-knots-from-file
  "Attempts to read the data in edn format from the file, and returns the
  unevaluated form.  Returns nil if read fails, or if the first form
  is not a ns declaration."
  [file]
  (with-open [rdr (java.io.PushbackReader. (io/reader file))]
    {:knot-file (str file)
     :knot (clojure.edn/read rdr)}))

(s/def ::file #(io/file %))
(s/def ::knot-file ::file)
(s/def ::knot coll?)

(s/fdef read-knots-from-file
        :args ::file 
        :ret (s/keys :req [::knot-file ::knot]))

(defn find-knots-in-dir
  "Searches recursively under dir for Clojure source files (.clj, .cljc).
  Returns a sequence of File objects, in breadth-first sort order."
  [^File dir]
  ;; Use sort by absolute path to get breadth-first search.
  (as-> (file-seq dir) x
    (filter knot-file? x)
    (sort-by #(.getAbsolutePath ^File %) x)
    (map read-knots-from-file x)))

(defn read-knots-from-jarfile-entry
  "Attempts to read a (ns ...) declaration from the named entry in the
  JAR file, and returns the unevaluated form.  Returns nil if the read
  fails, or if the first form is not a ns declaration."
  [^JarFile jarfile ^String entry-name]
  (let [f (.getEntry jarfile entry-name)]
    (with-open [rdr (PushbackReader.
                     (io/reader
                      (.getInputStream jarfile (.getEntry jarfile entry-name))))]
      {:knot-file (str f)
       :knot (clojure.edn/read rdr)})))

(defn find-knots-in-dir
  "Searches recursively under dir for Clojure source files (.clj, .cljc).
  Returns a sequence of File objects, in breadth-first sort order."
  [^File dir]
  ;; Use sort by absolute path to get breadth-first search.
  (as-> (file-seq dir) x
    (filter knot-file? x)
    (sort-by #(.getAbsolutePath ^File %) x)
    (map read-knots-from-file x)))

(defn read-knots-from-jarfile-entry
  "Attempts to read a (ns ...) declaration from the named entry in the
  JAR file, and returns the unevaluated form.  Returns nil if the read
  fails, or if the first form is not a ns declaration."
  [^JarFile jarfile ^String entry-name]
  (let [f (.getEntry jarfile entry-name)]
    (with-open [rdr (PushbackReader.
                     (io/reader
                      (.getInputStream jarfile (.getEntry jarfile entry-name))))]
      ({:knot-file (str f)
        :knot (clojure.edn/read rdr)}))))

(defn find-knots-in-jarfile
  "Searches the JAR file for Clojure source files containing (ns ...)
  declarations; returns the unevaluated ns declarations."
  [^JarFile jarfile]
  (filter identity
          (map #(read-knots-from-jarfile-entry jarfile %)
               (knot-files-in-jar jarfile))))

(defn find-all-knots
  "Searches a sequence of java.io.File objects (both directories and
  JAR files) for files ending with .knt. 
Returns a sequence of all found maps. Use with clojure.java.classpath to search Clojure's classpath."
  [files]
  (concat
   (mapcat find-knots-in-dir (filter #(.isDirectory ^File %) files))
   (mapcat find-knots-in-jarfile (jar-files files))))

(defn valid-contribution? [knot-file s form]
  (try
    (if (s/valid? s form)
      true
      (do (log/error (format "Contribution '%s' to %s from file %s is not valid: %s" form s knot-file (s/explain-data s form)))
          false))
    (catch Exception e (do (log/error e) false))))

(defn require-namespaces [all-key-ns]
  (doseq [n all-key-ns]
    (try
      (require n)
      (catch Exception e (log/error e)))))

(defn build-registry
  "Searches CLASSPATH (both directories and JAR files) for files named knot.edn.
A knot file is an edn file containing solely a single map which keys are namespace qualified keywords. These keys are pointing to corresponding clojure specs. 
The value mapped to a key 'k' is a seq 's' of values. Each value in 's' must be valid due to the spec of 'k'. Invalid values are ignored.
Returns a single map with all maps merged with conj."
  []
  (let [knot-maps (find-all-knots (cjc/classpath))
        all-key-ns (as-> (map :knot knot-maps) x (map keys x) (apply concat x) (map namespace x) (map symbol x) (set x))]
    (require-namespaces all-key-ns)
    (as-> knot-maps x 
      (map (fn [{:keys [knot-file knot] :as m}]
             (assoc m :knot (as-> knot y
                              (map (fn [[k v-seq]]
                                     (vector k (filter (fn [v] (valid-contribution? knot-file k v)) v-seq))) y)
                              (into {} y)))) x)
      (reduce (fn [a1 {:keys [file knot]}]
                (reduce (fn [a2 [k v]]
                          (if (contains? a2 k)
                            (update-in a2 [k] concat v)
                            (assoc-in  a2 [k] v))) a1 knot)) {} x))))


(defn contributions
  "Returns all contributions to 'extension-point-id'"
  [registry extension-point-id]
  (get registry extension-point-id))

(defn resolve-symbol
  [namespaced-symbol]
  (let [namespace  (namespace namespaced-symbol)
        name       (name namespaced-symbol)]
    (if (and (not (nil? namespace))
             (not (contains? (loaded-libs) (symbol namespace))))
      (require (symbol namespace)))
    (if-let [v (ns-resolve (symbol namespace) (symbol name))]
      (deref v))))
