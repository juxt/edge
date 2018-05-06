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
   [com.walmartlabs.lacinia :as lacinia]
   [edge.graphql :as graphql]
   [edge.system :as system]
   [integrant.repl :refer [clear halt prep init reset reset-all]]
   [integrant.repl.state :refer [system]]
   edge.reload
   [io.aviso.ansi]
   [yada.test :refer [response-for]]
   edge.yada.lacinia))

(when (System/getProperty "edge.load_krei")
  (println "[Edge] Loading krei")
  (require 'load-krei))

(when (System/getProperty "edge.reset_on_hup")
  (edge.reload/reset-on-hup))

(defn go []
  (let [res (integrant.repl/go)]
    (println (io.aviso.ansi/yellow
               (format "[Edge] Website can be browsed at http://%s/"
                       (-> system :edge/httpd :host))))
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
  (eval
    `(do
       (require 'figwheel-sidecar.repl-api)
       (figwheel-sidecar.repl-api/cljs-repl))))

;; REPL Convenience helpers

(defn graphql [q]
  (lacinia/execute (:edge.graphql/schema system) q nil system))

(defn graphql-stream [q]
  (edge.yada.lacinia/subscription-stream
    (:edge.graphql/schema system)
    q))

;; (graphql "query { person(id:102) { firstname phone surname }}")

(defn executor-stats []
  (->> system :edge/executor .getStats manifold.executor/stats->map))
