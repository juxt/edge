(ns pro.juxt.edge.exopaste.store
  (:require
    [integrant.core :as ig]))

(defn add-new-paste
  "Insert a new paste in the database, then return its UUID."
  [store content]
  (let [uuid (str (java.util.UUID/randomUUID))]
    (swap! store assoc (keyword uuid) {:content content})
    uuid))

(defn get-paste-by-uuid
  "Find the paste corresponding to the passed-in uuid, then return it."
  [store uuid]
  ((keyword uuid) @store))

(derive ::memory :pro.juxt.edge.exopaste/store)
(defmethod ig/init-key ::memory [_ init]
  (atom init))
