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

(def context-commands
  '((:make zensols.mkproj cli make-command
           classify classify-utterance-command)
    (:describe zensols.mkproj cli describe-command
               classify classify-utterance-command)))

(defn ^:private create-command-context []
  {:command-defs context-commands
   :single-commands {:version version-info-command}})

(defn -main [& args]
  (let [command-context (create-command-context)]
    (apply cli/process-arguments command-context args)))

;(-main)
;(-main "describe" "-s" "/Users/landes/view/template/lein")
(-main "make" "-s" "/Users/landes/view/template/lein" "-p" "package:zensols.nlp,project:clj-nl-parse")
;(-main "describe" "-s" "/Users/landes/view/template/lein")
