(ns juxt.calcite-play.core
  (:require
   [crux.api :as crux]
   [clojure.java.io :as io]
   [juxt.calcite-play.sw-load :as sw-load]
   juxt.calcite-play.schema-factory)
  (:import (crux.api ICruxAPI)
           (org.apache.calcite.jdbc CalciteConnection)))

(def ^crux.api.ICruxAPI node
  (crux/start-node {:crux.node/topology :crux.standalone/topology
	            :crux.node/kv-store "crux.kv.memdb/kv"
	            :crux.kv/db-dir "data/db-dir-3"
	            :crux.standalone/event-log-dir "data/eventlog-3"
	            :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))

(crux/submit-tx
 node
 (for [doc (sw-load/res->crux-docs (io/resource "swapi/resources/fixtures/planets.json"))]
   [:crux.tx/put doc]))

;
(vec (crux/q
      (crux/db node)
      '{:find [e climate]
        :where [[e :crux.db/id id]
                [e :climate climate]]}))

#_(java.sql.DriverManager/getConnection "jdbc:crux:db1")

#_(let [conn (java.sql.DriverManager/getConnection "jdbc:calcite:model=src/juxt/calcite_play/model.json")
      stmt (.createStatement conn)]
  (resultset-seq (.executeQuery stmt "select sku, description from product limit 2")))
