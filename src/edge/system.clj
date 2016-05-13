;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [aero.core :as aero]
   [com.stuartsierra.component :refer [system-map system-using using]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using)]
   [edge.server :refer [new-http-server]]
   edge.api
   edge.web
   [bidi.vhosts :refer [vhosts-model]]
   [yada.yada :refer [handler]]))

(defn config [profile]
  (let [f (io/file (System/getProperty "user.home") ".edge.edn")]
    (-> (if (.exists f) f (io/resource "config.edn"))
        (aero/read-config {:profile profile}))))

(defn configure-system [system config]
  (merge-with merge system config))

(defn try-routes
  "Call a 0-arity function to construct routes and recover from any
  exceptions"
  [routes-fn]
  (try
    (routes-fn)
    (catch Exception e
      [true (handler (ex-info "Exception occured when building routes" {} e))])))

(defn new-system-map []
  (system-map
   :http-server
   (new-http-server
    {:routes
     (vhosts-model
      [{:scheme :http
        :host "localhost:3000"}
       (try-routes (edge.web/content-routes {}))
       (try-routes (edge.api/api-routes {}))


       ;; Backstop
       [true (handler nil)]
       ])
     :port 3000})))

(defn new-dependency-map []
  {})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
