(ns edge.main
  "Main entry point"
  (:require clojure.pprint)
  (:gen-class))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require 'edge.system)
             (require 'edge.main)
             (require 'com.stuartsierra.component)

             (require 'clojure.java.browse)

             (println "Starting edge")

             (let [system (->
                           (edge.system/new-production-system)
                           com.stuartsierra.component/start)]

               (println "System started")
               (println "Ready...")))))
