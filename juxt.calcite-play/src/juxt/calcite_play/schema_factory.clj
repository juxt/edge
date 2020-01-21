(ns juxt.calcite-play.schema-factory
  (:require
   [crux.api :as crux]
   [juxt.calcite-play.node :refer [node]])
  (:gen-class
   :name juxt.calcite_play.SchemaFactory
   :implements [org.apache.calcite.schema.SchemaFactory])
  (:import
   (org.apache.calcite.sql.type SqlTypeName)))

(def schema ["SKU" "DESCRIPTION"])

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
               (def root root)
               (def filters filters)
               (def projects projects)
               (let [projects (or (seq projects) (range (count schema)))
                     syms (mapv gensym schema)
                     find* (mapv syms projects)
                     ->crux-where-clauses (fn ->crux-where-clauses
                                            [filter*]
                                            (condp = (.getKind filter*)
                                              ;; TODO: Assumes left is a column ref and
                                              ;; right is a constant, but doesn't enforce
                                              ;; that.
                                              org.apache.calcite.sql.SqlKind/EQUALS
                                              (let [left (.. filter* getOperands (get 0))
                                                    right (.. filter* getOperands (get 1))]
                                                [['?e
                                                  (keyword (get schema (.getIndex left)))
                                                  (str (.getValue2 right))]])
                                              org.apache.calcite.sql.SqlKind/AND
                                              (mapcat ->crux-where-clauses (.-operands filter*))))]
                 ;; This would do a query against Crux)
                 (org.apache.calcite.linq4j.Linq4j/asEnumerable
                   (mapv to-array
                         (doto
                           (crux/q
                             (crux/db node)
                             (doto
                               {:find find*
                                :where (vec
                                         (concat
                                           (mapcat ->crux-where-clauses filters)
                                           ;; Ensure they have all selected columns as attributes
                                           (mapv
                                             (fn [project]
                                               ['?e
                                                (keyword (get schema project))
                                                (get syms project)])
                                             projects)))}
                               prn))
                           prn))))))}))))

(defn -toString [this]
  "Crux Schema Factory")
