;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require [cljs.core.async :refer [<!]]
            [edge.net :as net]
            [edge.phonebook-app :as phonebook])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn init []
  (enable-console-print!)

  (when-let [section (. js/document (getElementById "phonebook"))]
    (println "Phonebook")
    (phonebook/init section))

  (let [sse-chan (net/sse-chan "/starwars")]
    (go-loop []
      (when-let [m (<! sse-chan)]
        (println {:a m}))
      (recur)))

  (println "Congratulations - your environment seems to be working"))
