(ns edge.aleph
  (:require [com.stuartsierra.component :refer [Lifecycle using]]
            [aleph.http :as http]
            [schema.core :as s]))

(defn create-handler [info]
  (fn [req]
    (let [data @(:model info)]
      {:body (format "%s %s" (:greeting data) (:recipient data))})))

(s/defrecord AlephWebserver [port :- (s/pred number?)
                             info :- {:model (s/atom {:recipient String
                                                      :greeting String})}
                             server]
  Lifecycle
  (start [component]
    (assoc component :server (http/start-server (create-handler info) component)))
  
  (stop [component]
    (when-let [server (:server component)]
      (.close server))
    component))

(defn new-aleph-webserver []
  (using
   (map->AlephWebserver {:port 3001})
   [:info]))
