(ns ${package}.hello-world
    (:require [zensols.actioncli.log4j2 :as lu]))

(defn- say-hello-world [text]
  (println (format "%s: Hello World!" text)))

(def hello-world-command
  "CLI command to invoke hello world"
  {:description "invoke hello world"
   :options
   [(lu/log-level-set-option)
    ["-s" "--sometext" "The utterance to parse"
     :required "TEXT"
     :default "Get ready"
     :validate [#(> (count %) 0) "No text given"]]]
   :app (fn [{:keys [sometext] :as opts} & args]
          (say-hello-world sometext))})
