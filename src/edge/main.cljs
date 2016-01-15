(ns edge.main
  (:require [rum.core :as rum :include-macros true]))

(defn init "The main entry point" []
  (enable-console-print!)
  (let [container (.getElementById js/document "content")]
    ;; Mount here
    )
  (println "edge reloaded!"))
