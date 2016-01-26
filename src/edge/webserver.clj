(ns edge.webserver
  (:require
   [aleph.http :as http]
   [bidi.ring :refer [make-handler redirect]]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [hiccup.core :refer [html]]
   [edge.chat :refer [get-channel]]
   [clojure.core.async :refer [chan >!! mult tap close!]]
   [schema.core :as s]
   [yada.yada :refer [yada resource]]))

(defn create-api []
  ["/"                   
   [
    ["" (redirect "index.html")]
    ["favicon.ico" (yada nil)]
    ["" (yada (io/file "target/dev"))]
    ]])

(s/defrecord Webserver [port :- (s/pred integer? "must be a port number!!")
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

(defn new-webserver []
  (using
   (map->Webserver {:port 3000})
   []))
