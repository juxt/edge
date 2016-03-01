;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [aero.core :as aero]
   [com.stuartsierra.component :refer [system-map system-using using]]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn config [profile]
  (let [f (io/file (System/getProperty "user.home") ".edge.edn")]
    (-> (if (.exists f) f (io/resource "config.edn"))
        (aero/read-config {:profile profile}))))

(defn configure-system [system config]
  (merge-with merge system config))

(defn new-system-map []
  (system-map))

(defn new-dependency-map []
  {})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
