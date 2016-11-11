(ns ^{:doc "A Clojure library to wrap Velocity: https://velocity.apache.org"
      :author "Paul Landes"}
    zensols.mkproj.velocity
  (:import [org.apache.velocity VelocityContext Template]
           [org.apache.velocity.app Velocity]
           [org.apache.velocity.runtime RuntimeSingleton])
  (:require [clojure.java.io :as io]))

;; initialize the Velocity system at library load
(Velocity/init)

(defn create-template-from-reader
  "Read and create a Velocity template."
  [name reader]
  (let [runtime-services (RuntimeSingleton/getRuntimeServices)
        node (.parse runtime-services reader name)]
    (doto (Template.)
      (.setRuntimeServices runtime-services)
      (.setData node)
      (.initDocument))))

(defn create-template-from-string
  "Read a Velocity template as a string and call it **name**, which seems to be
  useless."
  [name str]
  (->> str
       java.io.StringReader.
       (create-template-from-reader name)))

(defn create-template-from-file
  "Read a template from **file**."
  [file]
  (with-open [reader (io/reader file)]
    (create-template-from-reader (.getName file) reader)))

(defn apply-template
  "Do the interpolation and processing of a template using map **context** and
  return the result as a string."
  [context template]
  (let [vctx (VelocityContext. (java.util.HashMap. context))
        writer (java.io.StringWriter.)]
    (.merge template vctx writer)
    (.toString writer)))
