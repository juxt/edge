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
   ::webserver (new-aleph-webserver)
   ::database {:colour "RED"}))

(defn new-dependency-map []
  {::webserver {:database ::database}})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
