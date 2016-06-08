;; Copyright © 2016, JUXT LTD.

(ns edge.server
  (:require
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.yada :refer [handler resource] :as yada]
   edge.api
   edge.web))

(defn routes []
  [""
   [(edge.web/content-routes {})
    (edge.api/api-routes {})
    [true (handler nil)]]])

(defrecord WebServer [port template-caching? listener]
  Lifecycle
  (start [component]

    (selmer/set-resource-path! (io/resource "templates"))

    (if template-caching?
      (selmer.parser/cache-on!)
      (selmer.parser/cache-off!))

    ;; To provide a tag that gives access
    (selmer/add-tag! :url (fn [args context-map]
                            (when-let [ctx (:ctx context-map)]
                              (:href (yada/uri-for ctx
                                                   (keyword (first args))
                                                   {:route-params
                                                    (reduce (fn [acc [k v]] (assoc acc (keyword k) v)) {} (partition 2 (rest args)))})))))

    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host (format "localhost:%d" port)}
              (routes)])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (dissoc component :listener)))

(defn new-web-server []
  (map->WebServer {}))
