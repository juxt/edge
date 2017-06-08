(ns edge.xwing
  (:require [com.stuartsierra.component :refer [Lifecycle using]]
            [clojure.core.async :as a]))

(defn log [tc]
  (a/go
    (loop []
      (when-let [v (a/<! tc)]
        (println "Logging" v))
      (recur))))

(defrecord Xwing []
  Lifecycle
  (start [component]
    (let [c (a/chan 10)
          m (a/mult c)
          logging-tap (a/tap m (a/chan))]
      (log logging-tap)
      (assoc component :c c :m m)))

  (stop [component]
    (a/close! (:c component))
    component))

(defn new-xwing [m]
  (map->Xwing m))
