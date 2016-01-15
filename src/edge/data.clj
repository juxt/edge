(ns edge.data
  (:require [com.stuartsierra.component :refer [Lifecycle]]))

(defrecord Data []
  Lifecycle
  (start [component]
    (assoc component :model (atom {:greeting "Hello"
                                   :recipient "World!"})))
  (stop [component]
    component))

(defn new-data []
  (map->Data {}))



