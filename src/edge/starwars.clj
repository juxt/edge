(ns edge.starwars
  (:require [clj-http.client :as client]
            [com.stuartsierra.component :refer :all]
            [clojure.core.async :refer [<!! alts!! chan close! sliding-buffer timeout]]))

(defrecord Starwars []
  Lifecycle
  (start [this]
    (let [control-chan (chan (sliding-buffer 1))
          sse-chan (chan)]
      (assoc this
             :control-chan control-chan
             :sse-chan sse-chan)))
  (stop [{:keys [sse-chan] :as this}]
    (when sse-chan
      (close! sse-chan))
    this))
