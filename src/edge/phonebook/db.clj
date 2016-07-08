;; Copyright © 2015, JUXT LTD.

(ns edge.phonebook.db
  (:require
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [edge.phonebook.schema :refer [Phonebook PhonebookEntry]]
   [monger.core :as mg]
   [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(def phonebook-colname "phonebook")

(s/defn get-mongo-db [host db]
   (let [conn (mg/connect host)
         db   (mg/get-db conn db)]
     db))

(s/defn create-db [dbconfig
                   entries :- Phonebook]
  (let [db (get-mongo-db (:host dbconfig) (:dbname dbconfig))]
  db))


; We rely on ObjectId to generate the next db id, but we do not store it as such.
; Instead, we store it as its hash code. The reason is that yada needs an int as an id
; to generate a URI for the newly created resource. For now, we prefer this solution
; than to change the yada implementation to be able to handle ObjectIds.
(defn get-next-db-id []
  (.hashCode (ObjectId.)))

(defn add-entry
  "Add a new entry to the database. Returns the id of the newly added
  entry."
  [db entry]
  (dosync
    (let [record-id (get-next-db-id)]
      (mc/insert db phonebook-colname (assoc entry :_id record-id))
      record-id)))


(defn update-entry
  "Update a new entry to the database. Returns the id of the newly added
  entry."
  [db id entry]
  (dosync
   (mc/update-by-id db phonebook-colname id entry)
   id))


(defn delete-entry
  "Delete a entry from the database."
  [db id]
  (dosync
   (mc/remove-by-id db phonebook-colname id)
   id))

(s/defn get-entries :- Phonebook
  [db]
  (mc/find-maps db phonebook-colname))

;  TODO
(s/defn matches? [q :- String
                 entry :- PhonebookEntry]
  (some (partial re-seq (re-pattern (str "(?i:\\Q" q "\\E)")))
        (map str (vals (second entry)))))

; TODO
(s/defn search-entries :- Phonebook
  [db q]
  (let [entries (get-entries db)
        f (filter (partial matches? q) entries)]
    (into {} f)))

(s/defn get-entry :- (s/maybe PhonebookEntry)
  [db id]
  (mc/find-map-by-id db phonebook-colname id))


(s/defn count-entries :- s/Int
  [db]
  (mc/count db phonebook-colname))
