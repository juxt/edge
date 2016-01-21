(ns edge.main
  (:require
   [goog.dom :as gdom]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom])
  )

(defui HelloWorld
  Object
  (render [this]
          (dom/div nil "Hello")))

(def hello (om/factory HelloWorld))

(js/ReactDOM.render (hello) (gdom/getElement "app"))

(defn init []
  (enable-console-print!)
  (println "Hello world!"))
