;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require
   [reagent.core :as r]
   [edge.snake :as snake]))

(defn init []
  (enable-console-print!)
  (println "main init")

  (when-let [section (. js/document (getElementById "game"))]
    (println "SNAKE")
    (snake/init section)))
