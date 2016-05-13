;; Copyright © 2016, JUXT LTD.

(ns edge.server
  (:require
   [aleph.http :as http]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [schema.core :as s]
   [yada.yada :refer [yada resource] :as yada])
  (:import
   [bidi.vhosts VHostsModel]))

(s/defrecord HttpServer [port :- s/Int
                         routes
                         server]
  Lifecycle
  (start [component]
    (assoc component :server (yada/server routes {:port port})))
  (stop [component]
    (when-let [close (get-in component [:server :close])]
      (close))
    component))

(defn new-http-server [options]
  (map->HttpServer options))

