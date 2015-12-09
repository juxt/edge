(ns edge.trq
  (:require
   clojure.tools.reader
   [clojure.java.io :as io]
   [clojure.tools.logging :refer [infof errorf]]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :refer [closed? Channel]]
   edge.db
   [datomic.api :as d]
   [schema.core :as s])
  (:import
   [edge.db Database DbUriGenerator]
   [java.util.concurrent TimeUnit]))

(defn poll-until-val-of-closed
  [q ch]
  (loop []
    (when-not (closed? ch)
      (if-let [val (.poll q 1 TimeUnit/SECONDS)]
        val
        (recur)))))

(s/defrecord TransactionReportQueue
    [dburi :- DbUriGenerator
     ch :- (s/protocol Channel)
     database :- Database]
    component/Lifecycle
    (start [component]
      (let [uri (:uri dburi)
            conn (d/connect uri)
            ch (a/chan (a/dropping-buffer 10))]
        (a/thread
          (let [queue (d/tx-report-queue conn)]
            (loop []
              (when-let [tx-report (poll-until-val-of-closed queue ch)]
                (a/>!! ch tx-report)
                (recur)))))
        (assoc component :ch ch)))
    
    (stop [component]
      (when ch (a/close! ch))
      component))

(defn new-transaction-report-queue []
  (component/using
   (map->TransactionReportQueue {})
   [:database :dburi]))
