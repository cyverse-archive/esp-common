(defproject esp-common "0.1.0-SNAPSHOT"
  :description "Code common to the ESP components."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/data.json "0.1.2"]
                 [org.iplantc/clojure-commons "1.2.1-SNAPSHOT"]
                 [clj-http "0.5.3"]
                 [clj-time "0.4.3"]
                 [slingshot "0.10.1"]
                 [com.cemerick/url "0.0.6"]]
  :plugins [[lein-ring "0.7.4"]
            [swank-clojure "1.4.2"]]
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
