(ns edge.main
  (:require
   [goog.dom :as gdom]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom])
  )

#_(defui Garment
  Object
  (render [this]
          (dom/li nil (get (om/props this) :description))))

#_(def garment (om/factory Garment))

#_(defui GarmentList
  static om/IQuery
  (query [this]
    [:title :garments])
  
  Object
  (render [this]
          (let [{:keys [title garments]} (om/props this)]
            (dom/div nil
              (dom/h3 nil title)
              (dom/ul nil
                (for [g garments]
                  (dom/li nil (:description g))))             
              ))))

#_(def garment-list (om/factory GarmentList))

#_(def app-state {:title "Dominic"
                :garments [[:garments-by-sku 1]
                           [:garments-by-sku 2]
                           ]
                :garments-by-sku {1 {:description "Dress"}
                                  2 {:description "Shoes"}
                                  3 {:description "Bra"}
                                  4 {:description "Knickers"}
                                  5 {:description "Trousers"}}})

(def app-state (atom {:count 0}))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      {:value :not-found})))

(defn mutate [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:count]}
     :action #(swap! state update-in [:count] inc)}
    {:value :not-found}))

(defui Counter
  static om/IQuery
  (query [this]
         [:count])
  Object
  (render [this]
          (let [{:keys [count]} (om/props this)]
            (dom/div nil
        (dom/span nil (str "Count: " count))
        (dom/button
          #js {:onClick
               (fn [e] (om/transact! this '[(increment)]))}
          "Click me!")))))

(def reconciler
  (om/reconciler {:state app-state
                  :parser (om/parser {:read read :mutate mutate})}))

(defn init []
  (enable-console-print!)
  (om/add-root! reconciler Counter (gdom/getElement "app")))
