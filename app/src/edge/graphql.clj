(ns edge.graphql
  (:require
   [aleph.http :as http]
   [manifold.stream :as ms]
   [manifold.deferred :as md]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   [com.walmartlabs.lacinia :as lacinia]
   [com.walmartlabs.lacinia.schema :as schema]
   [com.walmartlabs.lacinia.util
    :refer
    [attach-resolvers attach-streamers]]
   [edge.event-bus :as bus]

   [edge.phonebook.db :as db]
   [integrant.core :as ig]
   edge.yada.lacinia
   [edge.yada.graphql-ws :refer [handle-incoming-ws-message]]
   [yada.yada :refer [handler resource] :as yada]))

;; ----------------------------------------------------------------------

(defn person [{:keys [edge.phonebook/db]}
              {:keys [id]}
              _]
  (assert db)
  (assert id)
  (db/get-entry db id))

(defn person-streamer [{:keys [edge/executor edge/event-bus]} _ cb]
  (assert event-bus)
  (let [cancel (bus/new-promise event-bus)
        phone (atom 100)]

    ;; Just testing - not yet useful
    (->
      (md/future
        (while (not (realized? cancel))
          (cb {:id (rand-nth (range 100 120))
               :person {:firstname (str (rand-int 1000))
                        :surname "bar"
                        :phone (swap! phone inc)}})
          (Thread/sleep 200))
        (log/debug "Task finished"))
      (md/onto executor))

    (fn cleanup []
      (log/debug "Closing streamer")
      (deliver cancel :cleanup-called))))

(defn schema []
  (-> (edn/read-string (slurp (io/resource "graphql-schema.edn")))
      (attach-resolvers {:person person})
      (attach-streamers {:stream-person person-streamer})
      schema/compile))

(defmethod ig/init-key :edge.graphql/schema
  [_ {:keys [edge/event-bus edge/executor edge.phonebook/db]}]
  (schema))

(defn routes [{:keys [edge.phonebook/db
                      edge.graphql/schema
                      edge/event-bus
                      edge/executor] :as config}]
  ;; GraphQL
  ["/"
   [
    ["graphql"
     (yada/resource
       {:methods
        {:post
         {:consumes "application/json"
          :produces "application/json"
          :response (fn [ctx]
                      (lacinia/execute schema (-> ctx :body :query) nil config))}}})]

    ["graphql-stream-sse"
     (yada/resource
       {:methods
        {:post
         {:consumes "application/json"
          :produces "text/event-stream"
          :response (fn [ctx]
                      (when-let [q (-> ctx :body :query)]
                        (ms/transform
                          (map json/encode)
                          (edge.yada.lacinia/subscription-stream schema q executor))))}}})]

    ["graphql-stream-ws"
     (yada/resource
       {:methods
        {:get
         {:consumes "application/json"
          :produces "application/json"
          :response
          (fn [ctx]
            (log/debug "Upgrading to WS")
            (let [ws-stream @(http/websocket-connection (:request ctx))
                  subscriptions (atom {})]
              ;; TODO: Try ms/consume
              (->
                (md/future
                  (loop []
                    (log/debug "Waiting for message...")
                    (when-let [msg @(ms/take! ws-stream)]
                      (log/debug "Got message:" msg)
                      (handle-incoming-ws-message
                        (json/decode msg keyword)
                        (merge config
                               {:edge.manifold/stream ws-stream
                                :edge.yada/ctx ctx
                                :edge.graphql/subscription-streams-by-id subscriptions}))
                      (recur))))
                (md/onto executor))))}}})]]])
