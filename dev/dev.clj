;; Copyright © 2016, JUXT LTD.

(ns dev
  (:require
   [bidi.bidi :as bidi]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer [run-all-tests]]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as a :refer [>! <! >!! <!! chan buffer dropping-buffer sliding-buffer close! timeout alts! alts!! go-loop]]
   [edge.system :refer (new-system-map new-dependency-map)]
   [reloaded.repl :refer [system init start stop go reset reset-all]]
   [schema.core :as s]))

(defn new-dev-system
  "Create a development system"
  []
  (component/system-using (new-system-map) (new-dependency-map)))

(reloaded.repl/set-init! new-dev-system)

(defn check
  "Check for component validation errors"
  []
  (let [errors
        (->> system
             (reduce-kv
              (fn [acc k v]
                (assoc acc k (s/check (type v) v)))
              {})
             (filter (comp some? second)))]

    (when (seq errors) (into {} errors))))

(defn test-all []
  (run-all-tests #"edge.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

;; REPL Convenience helpers

(defn get-port [] (-> system :webserver :port))
