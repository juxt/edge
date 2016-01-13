(ns edge.log-stream
  (:require
   clojure.tools.reader
   [clojure.java.io :as io]
   [clojure.tools.logging :refer [infof errorf]]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as a]
   edge.trq
   [clojure.core.async.impl.protocols :refer [closed? Channel]]
   [datomic.api :as d]
   [schema.core :as s])
  (:import [edge.trq TransactionReportQueue]
           [edge.db DbUriGenerator]
           [datomic.log LogValue]))

(s/defrecord LogStream [dburi :- DbUriGenerator
                        trq :- TransactionReportQueue]
  component/Lifecycle
  (start [component]
    (let [uri (:uri dburi)
          conn (d/connect uri)]
      (a/thread
        (let [ch (:ch trq)]
          (loop []
            (when-let [nudge (a/<!! ch)]
              (infof "Got a nudge, let's look into the log")
              (let [log (d/log conn)]
                (infof "log is %s" (pr-str (seq (d/tx-range log nil nil))))
                (recur))))))
      component))
  (stop [component] component))

(defn new-log-stream []
  (component/using
   (map->LogStream {})
   [:trq :dburi]))
