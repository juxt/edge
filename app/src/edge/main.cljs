;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require
   [reagent.core :as r]
   [edge.phonebook-app :as phonebook]))

(defn init []
  (enable-console-print!)

  (when-let [section (. js/document (getElementById "phonebook"))]
    (println "Phonebook")
    (phonebook/init section))

  (println "Congratulations - your environment seems to be working"))
