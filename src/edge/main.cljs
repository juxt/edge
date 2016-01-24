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
              (map garment (get-in (om/props this) [:garments]))))))

(def garment-list (om/factory GarmentList))

(def app-state {:title "Dominic"
                :garments [{:garments-by-sku 1}
                           ]
                :garments-by-sku {1 {:description "Dress"}
                                  2 {:description "Shoes"}
                                  3 {:description "Bra"}
                                  4 {:description "Knickers"}
                                  5 {:description "Trousers"}}})

(defn init []
  (enable-console-print!)
  (js/ReactDOM.render (garment-list (om/db->tree app-state))
                    (gdom/getElement "app"))
  (println "Hello world!"))
