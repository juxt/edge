;; Copyright © 2016, JUXT LTD.

(ns edge.webserver
  (:require
   [byte-streams :as b]
   [aleph.http :as http]
   [bidi.ring :refer [make-handler redirect]]
   [hiccup.core :refer [html]]
   [clojure.core.async :refer [chan >!! mult tap close! go go-loop <! >! timeout]]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [cheshire.core :as json]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [manifold.deferred :as d]
   [schema.core :as s]
   [om.next.server :as om]
   [camel-snake-kebab.core :as csk]
   [yada.yada :refer [yada handler resource swaggered]]
   [yada.yada :refer [as-resource]]))

;; TODO: Create a Webserver record here

(def bikepoints-data (-> "https://api.tfl.gov.uk/BikePoint" http/get deref :body b/to-string))

(defn bikepoints []
  {:bikepoints/by-id
   (into {}
         (for [{:keys [id] :as bikepoint}
               (-> bikepoints-data
                   (json/parse-string csk/->kebab-case-keyword))]
           [id
            (conj (select-keys bikepoint [:id :common-name])
                  [:additional-properties
                   (into {} (map (juxt (comp csk/->kebab-case-keyword :key) :value)
                                 (:additional-properties bikepoint)))])]))})

;; (take 4 (:bikepoints/by-id (bikepoints)))


(defrecord DatabaseConnection []
  Lifecycle
  (start [component]
    (println "Database starting!")
    component)
  (stop [component]
    (println "Database stopping")
    component))

(defn new-database []
  (->DatabaseConnection))

(defmulti readf (fn [env k params] k))

(defmethod readf :bikepoints/by-id
  [env _ params]
  (infof "env: %s" env)
  {:value (:bikepoints/by-id (bikepoints))})

(defn api []
  [""
   (swaggered
    ["/"
     [
      ["map" (handler (into {} (as-resource {:a "A"})))]
      ["logback" (handler (java.io.File. "resources/logback.xml"))]
      
      ["lines" (handler
                (resource
                 {:methods
                  {:get
                   {:produces "text/html"
                    :response
                    (fn [ctx]
                      (http/get "https://api.tfl.gov.uk/line/mode/tube/status")
                      
                      )}}}))]

      ["data" (handler
               (resource
                {:methods
                 {:post
                  {:consumes #{"application/transit+json" "application/transit+msgpack"}
                   :produces #{"application/transit+json" "application/transit+msgpack"}
                   :response (fn [ctx]
                               (let [parser (om/parser {:read readf})]
                                 (parser {} (:body ctx))))}}}))]
      
      ["" (handler (java.io.File. "target/dev"))]
      ]]
    {})])

(s/defrecord Webserver [port :- s/Int
                        database :- DatabaseConnection
                        server]

  Lifecycle
  (start [component]
    (try
      (let [server
            (http/start-server (make-handler (api))
                               {:port port})]
        (assoc component :server server))
      (catch Exception e
        (errorf e "Some failure")
        component)))

  (stop [component]
    (when server
      (.close server))
    component))

(defn new-webserver [port]
  (using
   (map->Webserver {:port port})
   [:database]))
