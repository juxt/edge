(ns juxt.calcite-play.core
  (:require
   juxt.calcite-play.schema-factory)
  (:import (crux.api ICruxAPI)
           (org.apache.calcite.jdbc CalciteConnection)))

(comment
  (let [conn (java.sql.DriverManager/getConnection "jdbc:calcite:model=src/juxt/calcite_play/model.json")
	stmt (.createStatement conn)]

    (resultset-seq
     (.executeQuery stmt "SELECT PERSON.NAME FROM PERSON"))

    #_(resultset-seq
       (.executeQuery stmt "SELECT PERSON.NAME FROM PLANET,PERSON WHERE PERSON.HOMEWORLD = PLANET.ID AND PLANET.CLIMATE = 'arid'"))))

;;(compile 'juxt.calcite-play.schema-factory)
