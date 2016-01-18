(ns edge.aleph
  (:require
   [aleph.http :as http]
   [bidi.ring :refer [make-handler redirect]]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [yada.yada :refer [yada]]))

(defn create-api []
  ["/"                   
   [
    ["" (redirect ::index)]
    ["index.html" (-> (yada "Welcome to EDGE") (assoc :id ::index))]
    ["favicon.ico" (yada nil)]
    ["" (yada (io/file "target/dev"))]]])

(s/defrecord AlephWebserver [port :- (s/pred number?)
                             server
                             api]
  Lifecycle
  (start [component]
    (let [api (create-api)]
      (assoc component
             :server (http/start-server (make-handler api) component)
             :api api)))
  
  (stop [component]
    (when-let [server (:server component)] (.close server))
    component))

(defn new-aleph-webserver []
  (using
   (map->AlephWebserver {:port 3000})
   []))
