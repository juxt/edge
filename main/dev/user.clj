;; Copyright © 2016-2018, JUXT LTD.

(ns user
  (:require
   [clojure.tools.namespace.repl :refer :all]
   [io.aviso.ansi]
   [integrant.repl.state]
   [io.aviso.ansi]
   [spyscope.core]))

(when (System/getProperty "edge.load_nrepl")
  (require 'nrepl))

(defn dev
  "Call this to launch the dev system"
  []
  (println "[Edge] Loading Clojure code, please wait...")
  (require 'dev)
  (when-not integrant.repl.state/system
    (println (io.aviso.ansi/bold-yellow "[Edge] Enter (go) to start the dev system")))
  (in-ns 'dev))

(defn fixed!
  "If, for some reason, the Clojure code in the project fails to
  compile - we still bring up a REPL to help debug the problem. Once
  the problem has been resolved you can call this function to continue
  development."
  []
  (refresh-all)
  (in-ns 'dev))
