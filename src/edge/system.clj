(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [edge.aleph :refer [new-aleph-webserver]]
   [edge.chat :refer [new-chat-channel]]
   [com.stuartsierra.component :refer (system-map system-using using)]
   ))

(defn new-system-map []
  (system-map
   ::webserver (new-aleph-webserver)
   ::chat-channel (new-chat-channel)))

(defn new-dependency-map []
  ;; the webserver has a 'using' declaration: [:chat]
  ;; the lunchbot has a 'using' declaration: [:lunch-requests]
  {::webserver {:messages ::chat-channel}})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
