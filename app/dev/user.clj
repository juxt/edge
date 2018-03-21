;; Copyright © 2016-2018, JUXT LTD.
(ns user
  (:require
    [clojure.tools.namespace.repl :refer :all]
    nrepl))

(defn fixed!
  []
  (refresh-all)
  (in-ns 'dev))

(defn dev
  []
  (println "[edge] Compiling code, please wait...")
  (require 'dev)
  (in-ns 'dev))
