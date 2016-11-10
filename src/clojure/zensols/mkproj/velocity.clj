(ns zensols.mkproj.velocity
  (:import [org.apache.velocity VelocityContext Template]
           [org.apache.velocity.app Velocity]
           [org.apache.velocity.runtime RuntimeSingleton])
  (:require [clojure.java.io :as io]))

(Velocity/init)

(defn create-template-from-reader [name reader]
  (let [runtime-services (RuntimeSingleton/getRuntimeServices)
        node (.parse runtime-services reader name)]
    (doto (Template.)
      (.setRuntimeServices runtime-services)
      (.setData node)
      (.initDocument))))

(defn create-template-from-string [name str]
  (->> str
       java.io.StringReader.
       (create-template-from-reader name)))

(defn create-template-from-file [file]
  (with-open [reader (io/reader file)]
    (create-template-from-reader (.getName file) reader)))

(defn apply-template [context template]
  (let [vctx (VelocityContext. (java.util.HashMap. context))
        writer (java.io.StringWriter.)]
    (.merge template vctx writer)
    (.toString writer)))
