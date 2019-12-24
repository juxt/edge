(ns juxt.calcite-play.core
  (:require
   [crux.api :as crux]
   juxt.calcite-play.schema-factory)
  (:import (crux.api ICruxAPI)
           (org.apache.calcite.jdbc CalciteConnection)))

(def ^crux.api.ICruxAPI node
  (crux/start-node {:crux.node/topology :crux.standalone/topology
	            :crux.node/kv-store "crux.kv.memdb/kv"
	            :crux.kv/db-dir "data/db-dir-1"
	            :crux.standalone/event-log-dir "data/eventlog-1"
	            :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))

(crux/submit-tx
 node
 [[:crux.tx/put
   {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
    :name "Pablo"
    :last-name "Picasso"}
   ]])

(crux/q
 (crux/db node)
 '{:find [e]
   :where [[e :name "Pablo"]]})

(let [conn (java.sql.DriverManager/getConnection "jdbc:calcite:model=src/juxt/calcite_play/model.json")
      stmt (.createStatement conn)]
  (resultset-seq (.executeQuery stmt "select sku, description from product limit 2")))
