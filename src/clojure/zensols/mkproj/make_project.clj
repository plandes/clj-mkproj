(ns zensols.mkproj.make-project
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml])
  (:require [zensols.mkproj.velocity :as v]
            [zensols.mkproj.config :as c]))

(def ^{:dynamic true :private true}
  *generate-config*
  "Top level template configuration")

(defn- write-template-reader [reader name dst-file]
  (let [{:keys [template-context]} *generate-config*]
    (with-open [writer (io/writer dst-file)]
      (binding [*out* writer]
        (log/debugf "template context: %s" template-context)
        (->> reader
             (v/create-template-from-reader name)
             (v/apply-template template-context)
             print)))))

(defn- write-template-file [src-file dst-file]
  (with-open [reader (io/reader src-file)]
    (write-template-reader reader (.getName src-file) dst-file)))

(defn- write-project-config [project-config dst-dir]
  (let [dst-file (->> (c/project-file-yaml) (io/file dst-dir))]
    (with-open [writer (io/writer dst-file)]
      (binding [*out* writer]
        (->> (yaml/generate-string project-config
                                   :dumper-options {:flow-style :block})
             println)))
    (log/infof "wrote project config file: %s" dst-file)))

(defn- process-file [src-file dst-file]
  (log/debugf "processing file: %s -> %s" src-file dst-file)
  (let [{:keys [generate]} *generate-config*
        to-match (.getPath src-file)
        exclude-tempify? (->> (:excludes generate)
                              (map #(re-find (re-pattern %) to-match))
                              (remove nil?)
                              first)
        dst-dir (.getParentFile dst-file)]
    (log/tracef "exclude on %s: %s" to-match exclude-tempify?)
    (if exclude-tempify?
      (io/copy src-file dst-file)
      (write-template-file src-file dst-file))
    (log/infof "wrote %s %s -> %s"
               (if exclude-tempify? "file" "template")
               (.getPath src-file) dst-file)))

(defn- unfold-dirs [cur-dir dst-dir]
  (if-not dst-dir
    cur-dir
    (let [[_ next-dir rest-dir] (re-find #"([^/]+)(\/.+)?" dst-dir)]
      (unfold-dirs (io/file cur-dir next-dir) rest-dir))))

(declare process-directory)

(defn- process-files [dst-dir src-files]
  (->> src-files
       (map (fn [src-file]
              (let [dst-file (io/file dst-dir (.getName src-file))]
               (cond (.isFile src-file)
                     (process-file src-file dst-file)
                     (.isDirectory src-file)
                     (process-directory src-file dst-file)))))
       doall))

(defn- process-directory [src-dir dst-dir]
  (log/debugf "processing dir: %s -> %s" src-dir dst-dir)
  (if (not (.exists src-dir))
    (throw (ex-info (format "Directory not found: %s" src-dir)
                    {:src-dir src-dir})))
  (log/infof "creating directory: %s" dst-dir)
  (.mkdirs dst-dir)
  (let [{:keys [template-context]} *generate-config*
        confs (->> (c/dir-configs template-context src-dir)
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
         (process-files dst-dir))
    (->> confs
         (map (fn [[src {:keys [destination]}]]
                (let [dst-dir (unfold-dirs dst-dir destination)
                      src-dir (io/file src-dir src)]
                  (log/debugf "dir interpolate: %s -> %s" src-dir dst-dir)
                  (process-directory src-dir dst-dir))))
         doall)))

(defn- make-project [src-dir override-fn]
  (let [env (c/project-environment src-dir override-fn)
        {template-directory :template-directory
         template-context :context} env
        {project-name "project-name"} template-context
        dst-dir (io/file template-directory)
        dst-project-file (io/file dst-dir (c/project-file-yaml))]
    (log/infof "creating new project %s in %s" project-name dst-dir)
    (.mkdirs dst-dir)
    (binding [*generate-config* (merge (dissoc env :template-context)
                                       {:template-context template-context})]
      (let [cfg {:project env}]
        (write-project-config cfg dst-dir))
      (process-directory (io/file src-dir c/project-file-name) dst-dir))))

(defn- create-mapped-override-fn [map]
  (fn [key def]
    (get map key)))

(->> (create-mapped-override-fn {:project "clj-nlp-parse"
                                 :package "zensols.nlparse"
                                 :sub-group "com.zensols.nlp"
                                 :template-directory "/d/exproj"})
     (make-project (io/file "/Users/plandes/view/template/lein/zen-lein")))
