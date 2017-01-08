(ns ^{:doc "Configuration template build read and creation."
      :author "Paul Landes"}
    zensols.mkproj.config
  (:import [java.util Properties])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml])
  (:require [zensols.mkproj.velocity :as v]))

(def project-file-name
  "Name of the top level project template build file."
  "make-proj")

(def ^:dynamic *project-environment-validations*
  "Portions of the project environment for which to validate existence."
  #{:template-directory :template-context :context :project})

(defn project-file-yaml
  "The project file found at the root of a build configuration.

  See [[project-file-name]]."
  []
  (format "%s.yml" project-file-name))

(defn project-dir-config
  "Name of a directory configuration file."
  []
  "dir-config.yml")

(defn- option-default [key override-fn def]
  [key (or (override-fn key def)
           (:default def)
           (:example def))])

(defn project-config
  "Parse the (yaml) project template build file."
  [src-dir]
  (with-open [reader (->> (project-file-yaml)
                          (io/file src-dir)
                          io/reader)]
    (->> reader slurp yaml/parse-string)))

(defn- validate-project-environment [src-dir project-environment]
  (log/debugf "validating environment <%s>" (pr-str project-environment))
  (let [{template-directory :template-directory
         template-context :context} project-environment
        {project "project"} template-context]
    (if (and (contains? *project-environment-validations* :template-directory)
             (not template-directory))
      (throw (ex-info "Missing :template-directory in %s" src-dir
                      {:src-dir src-dir})))
    (if (and (contains? *project-environment-validations* :template-context)
             (not template-context))
      (throw (ex-info (format "Missing :context in %s" src-dir)
                      {:src-dir src-dir})))
    (if (and (contains? *project-environment-validations* :project)
             (not project))
      (throw (ex-info (format "Missing :project in :context in %s" src-dir)
                      {:src-dir src-dir}))))
  project-environment)

(defn project-environment
  "Parse and create a indigestible configuration file ([[project-file-yaml]])."
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
                   top-level-mod)
            (validate-project-environment src-dir))))))

(defn create-template-config
  "Wrte a configuration file based on a project configuration file.

  See [[project-file-yaml]]."
  [src-dir out-property-file]
  (let [comments (format "generated from source directory %s" src-dir)]
    (with-open [writer (io/output-stream out-property-file)]
      (binding [*project-environment-validations*
                #{:template-directory :template-context :context :project}]
       (let [env (project-environment src-dir)
             template-directory (:template-directory env)]
         (->> env
              :context
              (#(merge % {"template-directory" template-directory
                          "source" (.getAbsolutePath src-dir)}))
              (#(doto (java.util.Properties.)
                  (.putAll %)
                  (.store writer comments)))))))
    (log/infof "wrote configuration file: %s" out-property-file)))

(defn- create-config-template
  "Create a Velocity template from a YAML file, apply and parse it."
  [template-context config-file]
  (->> (v/create-template-from-file config-file)
       (v/apply-template template-context)
       yaml/parse-string))

(defn load-props
  "Load a properties file."
  [file-name]
  (with-open [reader (io/reader file-name)] 
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) v])))))

(defn- dir-configs
  "If **src-dir** has contains a [[project-dir-config]] file, parse it and
  return the directory and file mappings."
  [template-context src-dir]
  (let [dir-conf-file (io/file src-dir (project-dir-config))]
    (when (.exists dir-conf-file)
      (->> (create-config-template template-context dir-conf-file)
           :package :generate))))

(defn directory-mappings
  "If **src-dir** has contains a [[project-dir-config]] file, parse it and
  return the file mappings.  This mapping is a string directory of a
  directory name in the project build that maps to a different directory name
  in the applied target."
  [template-context src-dir]
  (->> (dir-configs template-context src-dir)
       (map :directory-map)))

(defn file-mappings
  "If **src-dir** has contains a [[project-dir-config]] file, parse it and
  return the file mappings.  This mapping is a string file name in the project
  build that maps to a different file name in the *same directory* relative to
  the destination directory (see [[directory-mappings]])."
  [template-context src-dir]
  (->> (dir-configs template-context src-dir)
       (map :file-map)
       (#(zipmap (map :source %) (map :destination %)))))
