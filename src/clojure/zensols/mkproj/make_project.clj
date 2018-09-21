(ns ^{:doc "The library that orchestrates the creation of the templated target project."
      :author "Paul Landes"}
    zensols.mkproj.make-project
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml])
  (:require [zensols.mkproj.velocity :as v]
            [zensols.mkproj.config :as c]))

(def ^{:dynamic true :private true}
  *generate-config*
  "Top level template configuration")

(defn- write-template-reader
  "Apply and write to the file system the template contents found in
  **reader**.  The template context (parameters) come
  from [[*generate-context*]].  Write the output to **dst-file**.  The **name**
  parameter is usually useless for our usecase."
  [reader name dst-file]
  (let [{:keys [template-context]} *generate-config*]
    (with-open [writer (io/writer dst-file)]
      (binding [*out* writer]
        (log/debugf "template context: %s" template-context)
        (->> reader
             (v/create-template-from-reader name)
             (v/apply-template template-context)
             print)))))

(defn- write-template-file
  "Read a template from **src-file**, apply using
  context [[*generate-context*]] and write it to **dst-file**.

  See [[write-template-reader]]."
  [src-file dst-file]
  (with-open [reader (io/reader src-file)]
    (write-template-reader reader (.getName src-file) dst-file)))

(defn- write-project-config
  "Write the project configuration, **project-config** to the target templated
  project in **dst-dir**.  The file name comes from [[c/project-file-yaml]]."
  [project-config dst-dir]
  (let [dst-file (->> (c/project-file-yaml) (io/file dst-dir))]
    (with-open [writer (io/writer dst-file)]
      (binding [*out* writer]
        (->> (yaml/generate-string project-config
                                   :dumper-options {:flow-style :block})
             println)))
    (log/infof "wrote project config file: %s" dst-file)))

(defn- process-file
  "Process a source file (**src-file**), optionally apply the template if not
  an exclude file, and write it to the target templated
  project (**dst-file**)."
  [src-file dst-file]
  (let [{:keys [generate]} *generate-config*
        to-match (.getPath src-file)
        exclude-tempify? (->> (:excludes generate)
                              (map #(re-find (re-pattern %) to-match))
                              (remove nil?)
                              first)
        _ (log/debugf "processing file: %s -> %s (exclude=%s)"
                      to-match dst-file (not (nil? exclude-tempify?)))
        dst-dir (.getParentFile dst-file)]
    (log/tracef "exclude on %s: %s" to-match exclude-tempify?)
    (if exclude-tempify?
      (io/copy src-file dst-file)
      (write-template-file src-file dst-file))
    (log/infof "wrote %s %s -> %s"
               (if exclude-tempify? "file" "template")
               (.getPath src-file) dst-file)))

(defn- unfold-dirs
  "Create a nested `java.io.File` by composition (constructor) from a
  front-slash delimited path string.  The current directory is given by
  **cur-dir** and the destination path string is given in **dst-dir**."
  [cur-dir dst-dir]
  (if-not dst-dir
    cur-dir
    (let [[_ next-dir rest-dir] (re-find #"([^/]+)(\/.+)?" dst-dir)]
      (unfold-dirs (io/file cur-dir next-dir) rest-dir))))

(declare process-directory)

(defn- process-files
  "Process files **src-file** by optionally applying a template and writing
  them to the **dst-dir** destination directory."
  [template-context src-dir dst-dir src-files]
  (let [file-mappings (c/file-mappings template-context src-dir)]
    (log/debugf "file mappings: <%s>" (pr-str file-mappings))
    (->> src-files
         (map (fn [src-file]
                (let [src-name (.getName src-file)
                      dst-name (or (get file-mappings src-name) src-name)
                      dst-file (io/file dst-dir dst-name)]
                  (log/tracef "%s, %s, %s, %s"
                              (get file-mappings src-name) src-name
                              dst-name dst-file)
                  (cond (.isFile src-file)
                        (process-file src-file dst-file)
                        (.isDirectory src-file)
                        (process-directory src-file dst-file)))))
         doall)))

(defn- process-directory
  "Process a source directory **src-dir** by copying or templating and then
  copying files to destination directory **dst-dir**.  Then apply recursively
  to all directories."
  [src-dir dst-dir]
  (log/debugf "processing dir: %s -> %s" src-dir dst-dir)
  (if (not (.exists src-dir))
    (throw (ex-info (format "Directory not found: %s" src-dir)
                    {:src-dir src-dir})))
  (log/infof "creating directory: %s" dst-dir)
  (.mkdirs dst-dir)
  (let [{:keys [template-context]} *generate-config*
        confs (->> (c/directory-mappings template-context src-dir)
                   (map (fn [{:keys [source] :as m}]
                          (if m
                            {(name source) (dissoc m :source)})))
                   (apply merge))
        dir-entries (.listFiles src-dir)]
    (log/debugf "dir confs: %s" (pr-str confs))
    (->> dir-entries
         (remove (fn [file]
                   (or (and (.isFile file)
                            (= (.getName file) (c/project-dir-config)))
                       (contains? confs (.getName file)))))
         (process-files template-context src-dir dst-dir))
    (->> confs
         (map (fn [[src {:keys [destination]}]]
                (let [dst-dir (unfold-dirs dst-dir destination)
                      src-dir (io/file src-dir src)]
                  (log/debugf "dir interpolate: %s -> %s" src-dir dst-dir)
                  (process-directory src-dir dst-dir))))
         doall)))

(defn make-project
  "Create the project templated target found in **src-dir**.  This directory
  needs a [[project-file-yaml]] file and a directory
  called [[c/project-file-name]].

  You can change a parameter from what's given in the [[project-file-yaml]] by
  providing **override-fn**.  This function takes a key and a map with
  `description`, `example` and `default` keys from the project file and returns
  a value to use for that parameter."
  [src-dir override-fn]
  (log/infof "making project from %s" src-dir)
  (let [env (c/project-environment src-dir :override-fn override-fn)
        {template-directory :template-directory
         template-context :context} env
        {project "project"} template-context
        dst-dir (io/file (.getParentFile (io/file template-directory)) project)
        dst-project-file (io/file dst-dir (c/project-file-yaml))]
    (assert project)
    (log/infof "creating new project %s -> %s" src-dir dst-dir)
    (.mkdirs dst-dir)
    (binding [*generate-config* (merge (dissoc env :template-context)
                                       {:template-context template-context})]
      (process-directory (io/file src-dir c/project-file-name) dst-dir))))

(defn create-mapped-override-fn
  "Default override function for [[make-project]]."
  [map]
  (fn [key def]
    (get map key)))
