(ns zensols.mkproj.core
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as cli])
  (:require [zensols.mkproj.version :as ver])
  (:gen-class :main true))

(defn- version-info-action []
  (println (format "%s (%s)" ver/version ver/gitref)))

(defn- create-action-context []
  (cli/multi-action-context
   '((:describe zensols.mkproj.cli describe-command)
     (:config zensols.mkproj.cli create-config-command)
     (:make zensols.mkproj.cli make-command))
   :version-option (cli/version-option version-info-action)
   :default-arguments ["make"]))

(defn -main [& args]
  (lu/configure "mkproj-log4j2.xml")
  (cli/set-program-name "mkproj")
  (-> (create-action-context)
      (cli/process-arguments args)))
