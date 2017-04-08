;; Copyright © 2016, JUXT LTD.

(ns user
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.test :refer [run-all-tests]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.java.io :as io]
   [clojure.core.async :as a :refer [>! <! >!! <!! chan buffer dropping-buffer sliding-buffer close! timeout alts! alts!! go-loop]]
   [edge.system :as system]
   [integrant.repl :refer [clear go halt init reset reset-all]]
   [yada.test :refer [response-for]]))

(integrant.repl/set-prep! #(system/new-system :dev))

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
