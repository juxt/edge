(ns edge.lacinia
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [cheshire.core :as json]
   [edge.phonebook.db :as db]
   [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia :refer [execute]]))

(defn person [db]
  (fn [context {:keys [id] :as arguments} value]
    (merge
      (db/get-entry db id))))

(defn schema [db]
  (-> (edn/read-string (slurp (io/resource "schema.edn")))
      (attach-resolvers {:person (person db)})
      schema/compile))

(defn query [db q]
  (execute (schema db) q nil nil))
