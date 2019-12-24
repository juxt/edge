(ns juxt.calcite-play.schema-factory
  (:gen-class
   :name juxt.calcite_play.SchemaFactory
   :extends org.apache.calcite.schema.impl.AbstractSchema)
  (:import
   (org.apache.calcite.sql.type SqlTypeName)))

(defn -create [this parent-schema name operands]
  (let [operands (into {} operands)]
    (proxy [org.apache.calcite.schema.impl.AbstractSchema] []
      (getTableMap []
        {"PRODUCT"
         (proxy
             [org.apache.calcite.schema.impl.AbstractTable
              org.apache.calcite.schema.ProjectableFilterableTable]
             []
             (getRowType [type-factory]
               (.createStructType
                type-factory
                (seq
                 ;; I propose this table would be defined in a schema
                 ;; 'document' in Crux.
                 {"SKU" (.createSqlType type-factory SqlTypeName/VARCHAR)
                  "DESCRIPTION" (.createSqlType type-factory SqlTypeName/VARCHAR)})))
             (scan [root filters projects]
               ;; This would do a query against Crux
               (org.apache.calcite.linq4j.Linq4j/asEnumerable
                [(to-array ["123" "Snow boots"])
                 (to-array ["124" "Coffee"])
                 (to-array ["125" "Beer can"])
                 (to-array ["126" "Skipping rope"])])))}))))

(defn -toString [this]
  "Crux Schema Factory")
