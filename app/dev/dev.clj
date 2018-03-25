;; Copyright © 2016-2018, JUXT LTD.
(ns dev
  (:require
   [clojure.core.async :as a :refer [>! <! >!! <!! chan buffer dropping-buffer sliding-buffer close! timeout alts! alts!! go-loop]]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.test :refer [run-all-tests]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [edge.graphql :as graphql]
   [edge.system :as system]
   [figwheel-sidecar.repl-api]
   [integrant.repl :refer [clear halt prep init reset reset-all]]
   [integrant.repl.state :refer [system]]
   [io.aviso.ansi]
   [yada.test :refer [response-for]]
   edge.yada.lacinia))

(when (System/getProperty "edge.load_krei")
  (require 'load-krei))

(defn go []
  (let [res (integrant.repl/go)]
    (println (io.aviso.ansi/yellow
               (format "[Edge] Website can be browsed at http://%s/"
                       (-> system :edge.web-server :config :host))))
    (println (io.aviso.ansi/bold-yellow "[Edge] Now make code changes, then enter (reset) here"))
    res))

(integrant.repl/set-prep! #(system/new-system :dev))

(defn test-all []
  (run-all-tests #"edge.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

(defn cljs-repl
  "Start a ClojureScript REPL"
  []
  (figwheel-sidecar.repl-api/cljs-repl))


;; REPL Convenience helpers
(defn graphql [q]
  (edge.yada.lacinia/query
    (:edge.component/phonebook-db system)
    (:edge.component/graphql-schema system)
    q))

(defn graphql-stream [q]
  (edge.yada.lacinia/subscription-stream
    (:edge.component/phonebook-db system)
    (:edge.component/graphql-schema system)
    q))

;; (graphql "query { person(id:102) { firstname phone surname }}")
