(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [edge.aleph :refer [new-aleph-webserver]]
   [edge.data :refer [new-data]]))

(defn new-system-map [opts]
  (system-map
   ::web-server (new-aleph-webserver)
   ::data (new-data)))

(defn new-dependency-map []
  {::web-server {:info ::data}})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map opts)
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))
