(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [edge.aleph :refer [new-aleph-webserver]]
   [com.stuartsierra.component :refer (system-map system-using using)]
   ))

(defn new-system-map []
  (system-map
   ::webserver (new-aleph-webserver)))

(defn new-dependency-map []
  {})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
