(ns ${package}.core
  (:require [zensols.actioncli.parse :as cli]
            [zensols.actioncli.log4j2 :as lu])
  (:require [${group}.version])
  (:gen-class :main true))

(def ^:private version-info-command
  {:description "Get the version of the application."
   :options [["-g" "--gitref"]]
   :app (fn [{refp :gitref} & args]
          (println ${group}.version/version)
          (if refp (println ${group}.version/gitref)))})

(defn- create-command-context []
  {:command-defs '((:repl zensols.actioncli repl repl-command)
                   (:hello ${package} hello-world hello-world-command))
   :single-commands {:version version-info-command}})

(defn -main [& args]
  ;;(lu/configure "log4j2.xml")
  (let [command-context (create-command-context)]
    (apply cli/process-arguments command-context args)))
