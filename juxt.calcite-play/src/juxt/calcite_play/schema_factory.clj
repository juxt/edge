(ns juxt.calcite-play.schema-factory
  (:require
   [clojure.string :as string]
   [crux.api :as crux]
   [juxt.calcite-play.node :refer [node]])
  (:gen-class
   :name juxt.calcite_play.SchemaFactory
   :implements [org.apache.calcite.schema.SchemaFactory])
  (:import
   (org.apache.calcite.sql.type SqlTypeName)))

(defn- ->crux-where-clauses
  [schema filter*]
  (condp = (.getKind filter*)
    ;; TODO: Assumes left is a column ref and
    ;; right is a constant, but doesn't enforce
    ;; that.
    org.apache.calcite.sql.SqlKind/EQUALS
    (let [left (.. filter* getOperands (get 0))
          right (.. filter* getOperands (get 1))]
      [['?e
        (let [attr (-> (get schema (.getIndex left)) string/lower-case)]
          (if (= attr "id")
            :crux.db/id
            (keyword attr)))
        (str (.getValue2 right))]])
    org.apache.calcite.sql.SqlKind/AND
    (mapcat (partial ->crux-where-clauses schema) (.-operands filter*))))

(defn- ->crux-query
  [schema filters projects]
  (let [projects (or (seq projects) (range (count schema)))
        syms (mapv gensym schema)
        find* (mapv syms projects)]
    {:find find*
     :where (vec
              (concat
                (mapcat (partial ->crux-where-clauses schema) filters)
                ;; Ensure they have all selected columns as attributes
                (mapv
                  (fn [project]
                    ['?e
                     (let [attr (-> (get schema project) string/lower-case)]
                       (if (= attr "id")
                         :crux.db/id
                         (keyword attr)))
                     (get syms project)])
                  projects)))}))

(defn make-table [schema]
  (let [schema (conj schema "ID")]
    (proxy
        [org.apache.calcite.schema.impl.AbstractTable
         org.apache.calcite.schema.ProjectableFilterableTable]
        []
      (getRowType [type-factory]
        (.createStructType
         type-factory
         (seq
          (into {}
                (for [field schema]
                  [field (.createSqlType type-factory SqlTypeName/VARCHAR)])))))
      (scan [root filters projects]
        (org.apache.calcite.linq4j.Linq4j/asEnumerable
         (mapv to-array
               (crux/q
                (crux/db node)
                (doto (->crux-query schema filters projects) prn))))))))

(defn -create [this parent-schema name operands]
  (let [operands (into {} operands)]
    (proxy [org.apache.calcite.schema.impl.AbstractSchema] []
      (getTableMap []
        {"PLANET"
         (make-table ["NAME" "CLIMATE" "DIAMETER"])

         "PERSON"
         (make-table ["NAME" "HOMEWORLD"])}))))

(defn -toString [this]
  "Crux Schema Factory")
