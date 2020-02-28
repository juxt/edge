(ns juxt.calcite-play.node
  "House the crux node"
  (:require
    [crux.api :as crux]
    [clojure.java.io :as io]
    [juxt.calcite-play.sw-load :as sw-load]))

(def ^crux.api.ICruxAPI node
  (crux/start-node {:crux.node/topology :crux.standalone/topology
		    :crux.node/kv-store "crux.kv.memdb/kv"
		    :crux.kv/db-dir "data/db-dir-1"
		    :crux.standalone/event-log-dir "data/eventlog-1"
		    :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))

(crux/submit-tx
  node
  (for [doc (sw-load/crux-docs)]
    [:crux.tx/put doc]))


(comment

(crux/submit-tx
 node
 [[:crux.tx/put
   {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
    :name "Pablo"
    :last-name "Picasso"}
   #inst "2018-05-18T09:20:27.966-00:00"]]) ; valid time

(crux/q (crux/db node)
        '{:find [e]
          :where [[e :name "Pablo"]]})

)
