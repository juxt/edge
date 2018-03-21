;; Copyright © 2016-2018, JUXT LTD.

(ns user
  (:require
   clojure.tools.namespace.repl))

;; This is an old trick from Pedestal. When system.clj doesn't compile,
;; it can prevent the REPL from starting, which makes debugging very
;; difficult. This extra step ensures the REPL starts, no matter what.

(defn dev
  []
  (println "[edge] Compiling code, please wait...")
  (require 'dev)
  (in-ns 'dev))

(defn go
  []
  (println "Don't you mean (dev) ?"))

(println "[edge] When the prompt appears, enter (dev) to continue")
