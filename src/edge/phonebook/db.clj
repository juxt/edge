;; Copyright © 2015, JUXT LTD.

(ns edge.phonebook.db
  (:require
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [edge.phonebook.schema :refer [Phonebook PhonebookEntry]]
   [monger.core :as mg]
   [monger.collection :as mc]
   [monger.operators :refer :all])
  (:import [org.bson.types ObjectId]))

(def phonebook-colname "phonebook")

(s/defn get-mongo-db [host db]
   (let [conn (mg/connect host)
         db   (mg/get-db conn db)]
     db))

(s/defn create-db [dbconfig
                   entries :- Phonebook]
  (let [db (get-mongo-db (:host dbconfig) (:dbname dbconfig))]
  ; Add text index for free text search on phonebook entries
  (mc/ensure-index db phonebook-colname {
     :surname "text"
     :firstname "text"
   })
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
  "Get all entries"
  [db]
  (mc/find-maps db phonebook-colname))


; Fields of a phonebook entry to be searched using text search.
(def phonebook-entry-fields-to-be-searched
  (vector :surname :firstname))

(defn make-regex [field query]
  { field { $regex (str ".*"  query ".*") $options "i" }})

(defn make-regex-search-vector [query]
  (mapv
    (fn [field] (make-regex field query))
    phonebook-entry-fields-to-be-searched))

; We text search phonebook fields by doing a MongoDB regexp on individual fields in combination
; with the MongoDB $or operator.
; A better solution would be to use MongoDB's text index and perform a real free
; text search. Unfortunately, this version of Monger does not implement this MongoDB operator.
(s/defn search-entries :- Phonebook
  "Free text search on some entry fields"
  [db q]
  (mc/find-maps db phonebook-colname { $or (make-regex-search-vector q)}))

(s/defn get-entry :- (s/maybe PhonebookEntry)
  "Get entry with id"
  [db id]
  (mc/find-map-by-id db phonebook-colname id))


(s/defn count-entries :- s/Int
  "Count the entries"
  [db]
  (mc/count db phonebook-colname))
