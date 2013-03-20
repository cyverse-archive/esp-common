(defproject esp-common "0.1.1-SNAPSHOT"
  :description "Code common to the ESP components."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.iplantc/clojure-commons "1.4.1-SNAPSHOT"]
                 [cheshire "5.0.2"]
                 [clj-http "0.6.5"]
                 [clj-time "0.4.5"]
                 [slingshot "0.10.3"]
                 [com.cemerick/url "0.0.7"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]
                                  [lein-midje "3.0.0"]]}}
  :plugins [[lein-ring "0.7.4"]]
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
