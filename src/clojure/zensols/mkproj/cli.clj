(ns zensols.mkproj.cli
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.mkproj.config :as c]
            [zensols.mkproj.make-project :as m]))

(def ^:private default-config-file
  (io/file (System/getProperty "user.home") ".mkproj"))

(defn- make-from-properties [config-file src-dir opts]
  (log/infof "reading config file: %s" config-file)
  (let [props (->> opts
                   (merge (if (.exists config-file)
                            (c/load-props config-file))))]
    (->> (m/create-mapped-override-fn props)
         (m/make-project src-dir))))

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
            (make-from-properties config source opts)))})
