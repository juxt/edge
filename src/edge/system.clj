(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using using)]))

(defn new-system-map [opts]
  (system-map))

(defn new-dependency-map []
  {})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map opts)
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))
