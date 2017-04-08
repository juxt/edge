;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [integrant.core :as ig]
   ;; Hook in integrant multimethods:
   [edge.phonebook.db]
   [edge.selmer]
   [edge.web-server]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config
  "Read EDN config, with the given profile. See Aero docs at
  https://github.com/juxt/aero for details."
  [profile]
  (aero/read-config (io/resource "config.edn")
                    {:profile profile}))

(defn new-system
  "Construct a new system, configured with the given profile"
  [profile]
  (:ig/system (config profile)))
