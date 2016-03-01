;; Copyright © 2016, JUXT LTD.

(defproject juxt/edge "0.1.0-SNAPSHOT"
  :description "Project template"
  :url "http://github.com/juxt/edge"

  :pedantic? :abort

  :dependencies
  [
   ;; Infrastructure
   [com.stuartsierra/component "0.3.1"]
   [prismatic/schema "1.0.5"]
   [org.clojure/core.async "0.2.374"]

   ;; Logging
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.13"]
   [org.slf4j/jul-to-slf4j "1.7.13"]
   [org.slf4j/log4j-over-slf4j "1.7.13"]
   [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]

   ;; Config
   [aero "0.1.5"]


   ]

  :main edge.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [reloaded.repl "0.2.1"]]
                   :source-paths ["dev"]}})
