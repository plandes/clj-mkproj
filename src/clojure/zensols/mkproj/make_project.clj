(ns zensols.mkproj.make-project
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [clj-yaml.core :as yaml])
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.mkproj.velocity :as v]
            [zensols.mkproj.config :as c]))

(def ^{:dynamic true :private true}
  *generate-config*
  "Top level template configuration")

(def ^:private default-config-file
  (io/file (System/getProperty "user.home") ".mkproj"))

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
  (log/infof "making project from %s" src-dir)
  (let [env (c/project-environment src-dir override-fn)
        {template-directory :template-directory
         template-context :context} env
        {project "project"} template-context
        dst-dir (io/file template-directory)
        dst-project-file (io/file dst-dir (c/project-file-yaml))]
    (assert project)
    (clojure.pprint/pprint env)
    (log/infof "creating new project %s -> %s" project dst-dir)
    (.mkdirs dst-dir)
    (binding [*generate-config* (merge (dissoc env :template-context)
                                       {:template-context template-context})]
      (let [cfg {:project env}]
        (write-project-config cfg dst-dir))
      (process-directory (io/file src-dir c/project-file-name) dst-dir))
    ))

(let [dir (io/file "/Users/landes/view/template/lein")]
  (->> (create-mapped-override-fn {:project "clj-nlp-parse"
                                   :package "zensols.nlparse"
                                   :sub-group "com.zensols.nlp"
                                   :template-directory "/d/exproj"})
                                        ;(c/project-environment dir) clojure.pprint/pprint
       ;(make-project dir)
       ))

(defn- create-mapped-override-fn [map]
  (fn [key def]
    (get map key)))

(defn- make-from-properties [config-file src-dir opts]
  (log/infof "reading config file: %s" config-file)
  (let [props (->> opts
                   (merge (if (.exists config-file)
                            (c/load-props config-file))))]
    (->> (create-mapped-override-fn props)
         (make-project src-dir))))

(defn- params-to-map [str]
  (when str
    (->> (#(s/split str #","))
         (map #(->> % (re-find #"^(.+?):(.+)$") rest (apply hash-map)))
         (apply merge))))

(defn- validate-opts [{:keys [source param] :as opts}]
  (if (nil? source)
    (throw (ex-info (format "Missing source directory -s option")
                    {:option "-s"}))
    (-> opts
        (dissoc :level :config :source :param)
        (merge (params-to-map param)))))

(def describe-command
  "CLI command to invoke a parameter list"
  {:description "list all project configuration parameters"
   :options
   [["-s" "--source" (format "the source directory containing the %s file"
                             (c/project-file-yaml))
     :required "FILENAME"
     :validate [#(and % (.isDirectory %)) "Not a directory"]
     :parse-fn io/file]]
   :app (fn [{:keys [template] :as opts} & args]
          (try
            (validate-opts opts)
            (c/print-help template)
            (catch java.io.FileNotFoundException e
              (binding [*out* *err*]
                (println (.getMessage e))))
            (catch Exception e
              (binding [*out* *err*]
                (println (if ex-data
                           (.getMessage e)
                           (.toString e)))))))})

(def make-command
  "CLI command to invoke a template rollout of a project"
  {:description "generate a template rollout of a project"
   :options
   [(lu/log-level-set-option)
    ["-s" "--source" (format "the source directory containing the %s file"
                             (c/project-file-yaml))
     :required "FILENAME"
     :validate [#(and % (.isDirectory %)) "Not a directory"]
     :parse-fn io/file]
    ["-c" "--config" "location of the configuration file"
     :required "FILENAME"
     :default default-config-file
     :parse-fn io/file]
    ["-p" "--param" "list of project parameters (ie package:zensols.nlp,project:clj-nl-parse), see describe command"
     :required "PARAMETERS"]]
   :app (fn [{:keys [config source] :as opts} & args]
          (let [opts (validate-opts opts)]
            (make-from-properties config source opts)
            ))})
