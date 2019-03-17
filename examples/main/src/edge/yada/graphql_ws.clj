(ns edge.yada.graphql-ws
  (:require
   [cheshire.core :as json]
   [clojure.tools.logging :as log]
   [manifold.stream :as ms]))

;; https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md

;; From subscriptions-transport-ws/src/message-types.ts
(def ^:client GQL_CONNECTION_INIT "connection_init")
(def ^:server GQL_CONNECTION_ACK "connection_ack")
(def ^:server GQL_CONNECTION_ERROR "connection_error")
(def ^:client GQL_CONNECTION_TERMINATE "connection_terminate")
(def ^:client GQL_START "start")
(def ^:server GQL_DATA "data")
(def ^:server GQL_ERROR "error")
(def ^:server GQL_COMPLETE "complete")
(def ^:client GQL_STOP "stop")

(defn gql-error [id e]
  {:type GQL_ERROR :id id :payload (str e)})

(defmulti handle-incoming-ws-message (fn [msg ctx] (:type msg)))

(defmethod handle-incoming-ws-message GQL_CONNECTION_INIT
  [msg {:keys [edge.manifold/stream] :as ctx}]
  (log/debugf
    "GraphQL connection initiated from %s, acknowledging"
    (-> ctx :edge.yada/ctx :request :remote-addr))
  (ms/put! stream (json/encode {:type GQL_CONNECTION_ACK})))

(defmethod handle-incoming-ws-message GQL_START
  [msg {:keys [edge.manifold/stream
               edge.graphql/schema
               edge/executor
               edge.graphql/subscription-streams-by-id]
        :as ctx}]

  ;; TODO: Variables

  (let [id (some-> msg :id)
        q (some-> msg :payload :query)]

    (log/debug "Handle start, id" id ", q is" q)

    (try
      (when-not id (throw (ex-info "No id present" {})))
      (when-not q (throw (ex-info "No query present" {})))

      (let [source (edge.yada.lacinia/subscription-stream schema q ctx)]
        (swap! subscription-streams-by-id assoc id source)
        (ms/connect
          (ms/transform
            (map #(json/encode {:type GQL_DATA :id id :payload {:data %}}))
            source)
          stream))

      (catch Exception e
        (log/info e "Error starting GraphQL subscription")
        (ms/put! stream (json/encode (gql-error id e)))))))

(defmethod handle-incoming-ws-message GQL_STOP
  [msg {:keys [edge.graphql/subscription-streams-by-id]}]
  (let [id (some-> msg :id)]
    (log/debug "Should stop id" id)
    (when-let [source (get @subscription-streams-by-id id)]
      (log/debug "Found source for" id ", now closing it")
      (ms/close! source)
      (swap! subscription-streams-by-id dissoc id))))
