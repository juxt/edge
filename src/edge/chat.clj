(ns edge.chat
  (:require
   [clojure.core.async :refer [chan dropping-buffer close! go-loop timeout <! >!]]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [schema.core :as s]))

(defn get-channel [component]
  (:channel1 component))

(defn get-random-message []
  (rand-nth ["Hello Mum!" "Hello" "Hi" "Are you well?"
             "What matters for simplicity is that there's not interleaving."
             "Most of the biggest problems in software are problems of misconception."]))


(s/defrecord ChatChannel [buf channel1]
  Lifecycle
  (start [component]
    (let [ch (chan buf)]
      #_(go-loop []
        (<! (timeout 2000))
        (when (>! ch (get-random-message))
          (recur)))
      (assoc component :channel1 ch)))
  
  (stop [component]
    (when channel1 (close! channel1))
    component))


(defn new-chat-channel []
  (using
   (map->ChatChannel {:buf (dropping-buffer 10)})
   []))
