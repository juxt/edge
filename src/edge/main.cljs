;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require [cljs.core.async :refer [<!]]
            [edge.net :as net]
            [edge.phonebook-app :as phonebook]
            [edge.sw :as sw])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn init []
  (enable-console-print!)

  (when-let [section (. js/document (getElementById "phonebook"))]
    (println "Phonebook")
    (phonebook/init section))

  (when-let [section (. js/document (getElementById "starwars"))]
    (sw/init section))

  (println "Congratulations - your environment seems to be working"))
