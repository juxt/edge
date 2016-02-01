;; Copyright © 2016, JUXT LTD.

(defproject juxt/edge "0.1.0-SNAPSHOT"
  :description "A modular project created with lein new modular sse"
  :url "http://github.com/juxt/edge"

  :exclusions [com.stuartsierra/component]

  :dependencies
  [
   ;; Infrastructure
   [com.stuartsierra/component "0.3.1"]
   [reloaded.repl "0.2.1"]
   [prismatic/schema "1.0.4"]
   [org.clojure/core.async "0.2.374"]
   [org.clojure/tools.reader "0.10.0"]

   ;; Logging
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.13"]
   [org.slf4j/jul-to-slf4j "1.7.13"]
   [org.slf4j/log4j-over-slf4j "1.7.13"]
   [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]

   ;; Web
   [aleph "0.4.1-beta3"]
   [bidi "1.24.0"]
   [hiccup "1.0.5"]
   [org.omcljs/om "1.0.0-alpha28"]
   [yada "1.1.0-20160126.014942-13"]
   ]

  :main edge.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["dev"]}})
