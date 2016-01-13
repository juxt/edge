(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [edge.db :refer [new-dburi-generator new-database new-seeder]]
   [edge.trq :refer [new-transaction-report-queue]]
   [edge.log-stream :refer [new-log-stream]]))

(defn new-system-map [opts]
  (system-map
   ::dburi (new-dburi-generator)
   ::database (new-database)
   ::seeder (new-seeder)
   ::trq (new-transaction-report-queue)
   ::log (new-log-stream)))

(defn new-dependency-map []
  {::trq {:database ::database
          :dburi ::dburi}
   ::database {:dburi ::dburi}
   ::seeder {:database ::database
             :dburi ::dburi}
   ::log {:trq ::trq
          :dburi ::dburi}})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map opts)
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))
