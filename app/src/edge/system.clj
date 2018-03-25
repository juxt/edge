;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [edge.selmer]
   [edge.web-server]
   [edge.phonebook.db]
   [edge.graphql :as gql]
   [edge.event-bus :as bus]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config
  "Read EDN config, with the given profile. See Aero docs at
  https://github.com/juxt/aero for details."
  [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))

(defn new-system
  "Construct a new system, configured with the given profile"
  [profile]
  (:ig/system (config profile)))
