;; Copyright © 2016-2018, JUXT LTD.
(ns user
  (:require
    [clojure.tools.namespace.repl :refer :all]
    [nrepl]
    [io.aviso.ansi]))

(defn fixed!
  []
  (refresh-all)
  (in-ns 'dev))

(defn dev
  []
  (println "[Edge] Loading Clojure code, please wait...")
  (require 'dev)
  (eval
    '(when-not reloaded.repl/system
      (println (io.aviso.ansi/bold-yellow "[Edge] Enter (go) to start the dev system"))))
  (in-ns 'dev))
