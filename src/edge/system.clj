;; Copyright © 2016, JUXT LTD.

(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [edge.webserver :refer [new-webserver new-database]]))

(defn new-system-map []
  (system-map
   :webserver (new-webserver 3000)
   :postgres (new-database)
   ))

(defn new-dependency-map []
  {:webserver {:database :postgres}})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
