(defproject com.zensols.tools/mkproj "0.1.0-SNAPSHOT"
  :description "Generates boilerplate projects"
  :url "https://github.com/plandes/Natural Language Parse"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.10.1"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Boilerplate projects"}
          :output-path "target/doc/codox"}
  :git-version {:root-ns "zensols.mkproj"
                :path "src/clojure/zensols/mkproj"
                :version-cmd "git describe --match v*.* --abbrev=4 --dirty=-dirty"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :jar-exclusions [#".gitignore"]
  :exclusions [org.slf4j/slf4j-log4j12
               log4j/log4j
               ch.qos.logback/logback-classic]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.7"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.7"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                 [org.apache.logging.log4j/log4j-jcl "2.7"]

                 ;; templating
                 [org.apache.velocity/velocity "1.7"]
                 [org.apache.velocity/velocity-tools "2.0"]

                 ;; yaml parsing
                 [clj-yaml "0.4.0"]

                 ;; command line
                 [com.zensols.tools/actioncli "0.0.24"]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.mkproj.core"]
                                     [:id "mkproj"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:clojure-1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :uberjar {:aot [zensols.mkproj.core]}
             :appassem {:aot :all}
             :test
             {:jvm-opts
              ["-Dlog4j.configurationFile=test-resources/test-log4j2.xml"
               "-Xms4g" "-Xmx12g" "-XX:+UseConcMarkSweepGC"]}}
  :main zensols.mkproj.core)
