(ns edge.log
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

(s/defrecord Log [dburi :- DbUriGenerator
                  trq :- TransactionReportQueue
                  log :- LogValue]
  component/Lifecycle
  (start [component]
    (let [uri (:uri dburi)]
      (a/thread
        (let [ch (:ch trq)]
          (loop []
            (when-let [nudge (a/<!! ch)]
              (infof "Got a nudge, let's look into the log")
              (recur)))))
      (let [log (d/log (d/connect uri))]
        (infof "Log is type %s" (type log))
        (assoc component :log log))))
  (stop [component] component))

(defn new-log []
  (component/using
   (map->Log {})
   [:trq :dburi]))
