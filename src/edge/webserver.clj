;; Copyright © 2016, JUXT LTD.

(ns edge.webserver
  (:require
   [aleph.http :as http]
   [bidi.ring :refer [make-handler redirect]]
   [hiccup.core :refer [html]]
   [clojure.core.async :refer [chan >!! mult tap close! go go-loop <! >! timeout]]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [schema.core :as s]
   [yada.yada :refer [yada resource]]))

(defn make-api []
  ["/"
   [["" (redirect "index.html")]
    ["favicon.ico" (yada nil)]
    ["" (yada (io/file "target/dev"))]]])

(s/defrecord Webserver [port :- (s/pred integer? "must be a port number")]
  Lifecycle
  (start [component]
    (assoc component
           :server (http/start-server (make-handler (make-api)) component)))
  
  (stop [component]
    (when-let [server (:server component)] (.close server))
    component))

(defn new-webserver []
  (using
   (map->Webserver {:port 3000})
   []))

