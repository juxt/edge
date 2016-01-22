(ns edge.main
  (:require
   [goog.dom :as gdom]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom])
  )

(defui Garment
  Object
  (render [this]
          (dom/li nil (get (om/props this) :description))))

(def garment (om/factory Garment))

(defui GarmentList
  Object
  (render [this]
          (dom/div nil
            (dom/h3 nil (get-in (om/props this) [:title]))
            (dom/ul nil
              (map garment (get-in (om/props this) [:garments])))
          )))

(def garment-list (om/factory GarmentList))

(defn init []
  (enable-console-print!)
  (js/ReactDOM.render (garment-list {:title "Dominic"
                                     :garments [{:description "Dress"}
                                                {:description "Shoes"}
                                                {:description "Bra"}
                                                {:description "Knickers"}]})
                    (gdom/getElement "app"))
  (println "Hello world!"))
