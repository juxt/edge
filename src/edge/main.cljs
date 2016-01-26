 (ns edge.main
   (:require [om.next :as om :refer-macros [defui]]
             [om.dom :as dom]
             [goog.dom :as gdom]))

(def app-state
  {:app/title "Here is my title"})

(defui Garment)

(defui RootView
  Object
  (render [this] (dom/h2 nil (:app/title (om/props this)))))

(defn init []
  (enable-console-print!)
  (println "Congratulations - your environment seems to be working")
  (let [reconciler (om/reconciler {:status app-state})]
    (om/add-root! reconciler RootView (gdom/getElement "app")))
  )

