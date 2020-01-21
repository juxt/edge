(ns juxt.calcite-play.core
  (:require
   juxt.calcite-play.schema-factory)
  (:import (crux.api ICruxAPI)
           (org.apache.calcite.jdbc CalciteConnection)))

(comment
  (require '[juxt.calcite-play.node :refer [node]]
	   '[crux.api :as crux])
   
  (crux/submit-tx
    node
    (map
      (fn [x]
	[:crux.tx/put
	 (merge
	   {:crux.db/id (keyword "product" (str "sku-" (:sku x)))}
	   x)])
      [{:sku "123", :description "Snowy boots"}
       {:sku "124", :description "Coffee"}
       {:sku "125", :description "Beer can"}
       {:sku "126", :description "Skipping rope"}])))

(comment
  (let [conn (java.sql.DriverManager/getConnection "jdbc:calcite:model=src/juxt/calcite_play/model.json")
	stmt (.createStatement conn)]
    (resultset-seq (.executeQuery stmt "select sku, description from product where sku = '123'")))
  (let [conn (java.sql.DriverManager/getConnection "jdbc:calcite:model=src/juxt/calcite_play/model.json")
	stmt (.createStatement conn)]
    (resultset-seq (.executeQuery stmt "select name from planet"))))
