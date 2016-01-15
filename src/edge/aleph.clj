(ns edge.aleph
  (:require [com.stuartsierra.component :refer [Lifecycle using]]
            [aleph.http :as http]
            [bidi.ring :refer [make-handler redirect files]]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [schema.core :as s]
            [yada.yada :refer [yada resource]]))

(defn api [info]
  ["/"; a list of routes
   [
    ["hello" (yada
              (resource
               {:id ::index
                :methods
                {:get
                 {:produces "text/html"
                  :response (fn [ctx]
                              (let [data @(:model info)]
                                (html [:h1
                                       (format "%s %s" (:greeting data) (:recipient data))])))}}}))]

    ;;["" (redirect ::index)]

    ["index.html" (yada (io/file "target/dev/index.html"))]
    
    ["" (files {:dir "target/dev"})]

    ;; Catch all with a not found!
    [true (yada nil)]]])

(s/defrecord AlephWebserver [port :- (s/pred number?)
                             info :- {:model (s/atom {:recipient String
                                                      :greeting String})}
                             server]
  Lifecycle
  (start [component]
    (let [api (api info)]
      (assoc component
             :server (http/start-server (make-handler api) component)
             :api api)))
  
  (stop [component]
    (when-let [server (:server component)]
      (.close server))
    component))

(defn new-aleph-webserver []
  (using
   (map->AlephWebserver {:port 3000})
   [:info]))
