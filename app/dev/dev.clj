;; Copyright © 2016-2018, JUXT LTD.
(ns dev
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.test :refer [run-all-tests]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as a :refer [>! <! >!! <!! chan buffer dropping-buffer sliding-buffer close! timeout alts! alts!! go-loop]]
   [edge.system :as system]
   [reloaded.repl :refer [system init start stop reset reset-all]]
   [yada.test :refer [response-for]]
   [io.aviso.ansi]))

(when (System/getProperty "edge.load_krei")
  (require 'load-krei))

(defn new-dev-system
  "Create a development system"
  []
  (component/system-using
   (system/new-system-map (system/config :dev))
   (system/new-dependency-map)))

(defn go []
  (let [res (reloaded.repl/go)]
    (println (io.aviso.ansi/yellow (format "[Edge] Website can be browsed at http://%s/" (-> system :web-server :host))))
    (println (io.aviso.ansi/bold-yellow "[Edge] Now make code changes, then enter (reset) here"))
    res))

(reloaded.repl/set-init! new-dev-system)

(defn test-all []
  (run-all-tests #"edge.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

(defn cljs-repl
  "Start a ClojureScript REPL"
  []
  (eval
   '(do (in-ns 'boot.user)
        (start-repl))))


;; REPL Convenience helpers
