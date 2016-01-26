(ns edge.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [edge.webserver :refer [new-webserver]]
   [edge.chat :refer [new-chat-channel]]
   [com.stuartsierra.component :refer (system-map system-using using)]))

(defn new-system-map []
  (system-map
   ::webserver (new-webserver)))

(defn new-dependency-map []
  {::webserver {}})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map)
      (system-using (new-dependency-map))))
