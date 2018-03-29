(ns edge.graphql
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util
    :refer
    [attach-resolvers attach-streamers]]
   [edge.event-bus :as bus]
   [edge.phonebook.db :as db]
   [integrant.core :as ig]))

;; ----------------------------------------------------------------------

(defn person [{:keys [db]}
              {:keys [id] :as arguments}
              value]
  (assert db)
  (assert id)
  (db/get-entry db id))

(defn person-streamer [event-bus]
  (fn [_ _ cb]
    (let [cancel (bus/new-promise event-bus)
          phone (atom 100)]
      (future
        (while (not (realized? cancel))
          (cb {:firstname "foo2"
               :surname "bar"
               :phone (swap! phone inc)})
          (Thread/sleep 200)))

      (fn cleanup []
        (debug "Closing streamer")
        (deliver cancel :cleanup-called)))))

(defn schema [db event-bus]
  (-> (edn/read-string (slurp (io/resource "graphql-schema.edn")))
      (attach-resolvers {:person person})
      (attach-streamers {:stream-person (person-streamer event-bus)})
      schema/compile))

(defmethod ig/init-key :edge/graphql-schema
  [_ {:edge/keys [event-bus]
      :edge.phonebook/keys [db]}]
  (schema db event-bus))
