;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [system-map system-using]]
   [edge.selmer :refer [new-selmer]]
   [edge.server :refer [new-web-server]]
   [edge.phonebook.db :as db]))

(defn config [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))

(defn configure-system [system config]
  (merge-with merge system config))

(defn new-system-map []
  (system-map
   :web-server (new-web-server)
   :selmer (new-selmer)
   :db (db/create-db {})))

(defn new-dependency-map []
  {})

(defn new-system
  "Construct a new system with the given profile"
  [profile]
  (-> (new-system-map profile)
      (configure-system (config profile))
      (system-using (new-dependency-map))))
