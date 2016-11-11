(ns zensols.mkproj.config
  (:import [java.util Properties])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml])
  (:require [zensols.mkproj.velocity :as v]))

(def project-file-name
  "Name of the top level project file."
  "make-proj")

(defn project-file-yaml []
  (format "%s.yml" project-file-name))

(defn project-dir-config []
  "dir-config.yml")

(defn- option-default [key override-fn def]
  [key (or (override-fn key def)
           (:default def)
           (:example def))])

(defn project-config
  [src-dir]
  (with-open [reader (->> (project-file-yaml)
                          (io/file src-dir)
                          io/reader)]
    (->> reader slurp yaml/parse-string)))

(defn project-environment
  ([src-dir]
   (project-environment src-dir (constantly nil)))
  ([src-dir override-fn]
   (letfn [(zip-opts [key-fn ctx]
             (zipmap (map #(-> % first key-fn) ctx)
                     (map second ctx)))]
     (let [proj (->> (project-config src-dir) :project)
           top-level-mod (->> [:template-directory]
                              (map #(option-default % override-fn (get proj %)))
                              (zip-opts identity))
           top-level-vert [:generate]]
       (->> (:context proj)
            (map (fn [[k def]]
                   (option-default k override-fn def)))
            (zip-opts name)
            (hash-map :context)
            (merge (->> top-level-vert
                        (map #(hash-map % (get proj %)))
                        (apply merge))
                   top-level-mod))))))

(defn- create-config-template [template-context config-file]
  (->> (v/create-template-from-file config-file)
       (v/apply-template template-context)
       yaml/parse-string))

(defn dir-configs [template-context src-dir]
  (let [dir-conf-file (io/file src-dir (project-dir-config))]
    (when (.exists dir-conf-file)
      (->> (create-config-template template-context dir-conf-file)
           :package :generate (map :directory-map)))))

(defn print-help [src-dir]
  (->> (project-config src-dir)
       :project :context
       (map (fn [[op {:keys [description example default]}]]
              (println (format "%s: %s (eg %s)%s"
                               (name op) description example
                               (if default
                                 (format ", default: <%s>" default)
                                 "")))))))
