;; Copyright © 2015, JUXT LTD.

(ns edge.phonebook.db
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [datomic.api :as d]
   [clojure.set :as set]))

;; TODO: Support namespaced keywords in front-end
(defn normalize [m]
  (reduce-kv (fn [acc k v] (assoc acc (keyword (name k)) v)) {} m))

(defn denormalize [m]
  (reduce-kv (fn [acc k v] (assoc acc (keyword "phonebook" (name k)) v)) {} m))

(defn add-entry
  "Add a new entry to the database. Returns the id of the newly added
  entry."
  [db entry]
  (let [tempid (d/tempid :db.part/user)
        result @(d/transact (:conn db)
                            [(assoc (denormalize entry) :db/id tempid)])]
    (d/resolve-tempid (:db-after result) (:tempids result) tempid)))

(defn update-entry
  "Update a new entry to the database."
  [db id entry]
  @(d/transact (:conn db)
               [(assoc (denormalize (dissoc entry :id)) :db/id id)])
  entry)

(defn delete-entry
  "Delete a entry from the database."
  [db id]
  (d/transact (:conn db)
              [[:db.fn/retractEntity id]]))

(defn get-entries
  [db]
  (let [db (d/db (:conn db))]
    (into {}
          (map (juxt :db/id normalize)
               (d/pull-many db '[*]
                            (d/q
                             '[:find [?e ...]
                               :in $
                               :where
                               [?e :phonebook/phone _]]
                             db))))))

(defn matches? [q entry]
  (some (partial re-seq (re-pattern (str "(?i:\\Q" q "\\E)")))
        (map str (vals (second entry)))))

(defn search-entries
  [db q]
  (let [entries (get-entries db)
        f (filter (partial matches? q) entries)]
    (into {} f)))

(defn get-entry
  [db id]
  (normalize (d/pull (d/db (:conn db)) '[*] id)))

(defn count-entries
  [db]
  (count @(:phonebook db)))

(defrecord Database [uri entries]
  Lifecycle
  (start [component]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (d/transact
       (d/connect uri)
       (edn/read-string (slurp (io/resource "schema.edn"))))
      (d/transact conn (vals entries))
      (assoc component :conn conn)))

  (stop [component]
    (d/delete-database uri)
    component))

(defn new-database [m]
  (map->Database m))
