(ns ^{:doc "Command line interface for the app."
      :author "Paul Landes"}
    zensols.mkproj.cli
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:require [zensols.mkproj.config :as c]
            [zensols.mkproj.make-project :as m]))

(def ^:dynamic *dump-jvm-on-error* true)

(def ^:private default-config-file
  "The default configuration file to get parameters for project configuration."
  (io/file "mkproj.properties"))

(defn- config-properties
  "Return the configuration properties as a map."
  [{:keys [config] :as opts}]
  (log/debugf "config properties: opts=<%s>" opts)
  (->> opts
       (merge (if (.exists config)
                (c/load-props config)))))

(defn- make-from-properties
  "Create the project templated target found in **src-dir** using command line
  options **opts** and project configuration **config**."
  [config src-dir opts]
  (log/infof "reading config file: %s" config)
  (->> (config-properties opts)
       m/create-mapped-override-fn
       (m/make-project src-dir)))

(defn- params-to-map
  "Create a map from a key/value pair embedded string."
  [str]
  (when str
    (->> (#(s/split str #","))
         (map #(->> % (re-find #"^(.+?):(.+)$") rest (apply hash-map)))
         (apply merge))))

(defn- handle-exception
  "Handle exceptions thrown from CLI commands."
  [e]
  (if (instance? java.io.FileNotFoundException e)
    (binding [*out* *err*]
      (println (.getMessage e)))
    (binding [*out* *err*]
      (println (if ex-data
                 (.getMessage e)
                 (.toString e)))))
  (if *dump-jvm-on-error*
    (System/exit 1)
    (throw e)))

(defn print-describe
  "Print all project build parameters based on a [[project-file-yaml]] found in
  **src-dir** as markdown."
  [src-dir]
  (let [proj (-> (c/project-config src-dir) :project)
        pname (:name proj)]
    (println pname)
    (println (apply str (repeat (count pname) \-)))
    (println (:description proj))
    (println)
    (println "## Parameters")
    (->> proj :context
         (map (fn [[op {:keys [description example default]}]]
                (println (format "  * %s: %s (eg %s)%s"
                                 (name op) description example
                                 (if default
                                   (format ", default: <%s>" default)
                                   "")))))
         doall)))

(defn- validate-opts
  "Throw an exception if we're missing options and return a map that's suitable
  as a Velocity context."
  [{:keys [source param] :as opts}]
  (-> (if-not (nil? source)
        opts
        (let [{:keys [source] :as opts} (config-properties opts)]
          (if (nil? source)
            (throw (ex-info (format "Missing source directory -s option")
                            {:option "-s"})))
          opts))
      (dissoc :level :param)
      (merge (params-to-map param))
      (#(do (log/debugf "val opts: <%s>" opts) %))))

(def ^:private src-option
  ["-s" "--source" (format "the source directory containing the %s file"
                           (c/project-file-yaml))
   :required "FILENAME"
   :validate [#(and % (.isDirectory %)) "Not a directory"]
   :parse-fn io/file])

(def describe-command
  "CLI command to invoke a parameter list"
  {:description "list all project info and configuration parameters as markdown"
   :options
   [src-option]
   :app (fn [{:keys [source] :as opts} & args]
          (try
            (validate-opts opts)
            (print-describe source)
            (catch Exception e
              (handle-exception e))))})

(def create-config-command
  "CLI command to create a project configuration file"
  {:description "create a project configuration file (use -c with make command)"
   :options
   [src-option
    ["-d" "--destination" (format "the output file"
                                  (c/project-file-yaml))
     :required "FILENAME"
     :default default-config-file
     :validate [#(and % (.isDirectory %)) "Not a directory"]
     :parse-fn io/file]]
   :app (fn [{:keys [source destination] :as opts} & args]
          (try
            (validate-opts opts)
            (c/create-template-config source destination)
            (catch Exception e
              (handle-exception e))))})

(def make-command
  "CLI command to invoke a template rollout of a project"
  {:description "generate a template rollout of a project"
   :options
   [src-option
    ["-c" "--config" "location of the properties configuration file"
     :required "FILENAME"
     :default default-config-file
     :parse-fn io/file]
    ["-p" "--param" "list of project parameters (ie package:zensols.nlp,project:clj-nl-parse), see describe command"
     :required "PARAMETERS"]]
   :app (fn [{:keys [config source] :as opts} & args]
          (let [{:keys [source] :as opts} (validate-opts opts)]
            (log/debugf "config: %s, source: %s, opts: <%s>"
                        config source opts)
            (try (make-from-properties config source opts)
                 (catch Exception e
                   (handle-exception e)))))})
