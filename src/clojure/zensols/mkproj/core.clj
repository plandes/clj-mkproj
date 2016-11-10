(ns zensols.mkproj.core
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as cli])
  (:require [mkproj.version :as ver])
  (:gen-class :main true))


(def ^:private version-info-command
  {:description "Get the version of the application."
   :options [["-g" "--gitref"]]
   :app (fn [{refp :gitref} & args]
          (println ver/version)
          (if refp (println ver/gitref)))})
;
(def context-commands
  ;; '((:single oplab.langid-class
  ;;            classify classify-utterance-command)
  ;;   (:multi oplab.langid-class
  ;;           process classify-file-command))
  )

(defn ^:private create-command-context []
  {:command-defs (concat '((:repl zensols.actioncli repl repl-command))
                         context-commands)
   :single-commands {:version version-info-command}})

(defn -main [& args]
  (lu/configure "mkproj-log4j2.xml")
  (let [command-context (create-command-context)]
    (apply cli/process-arguments command-context args)))
