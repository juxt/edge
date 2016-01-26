;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require
   [cognitect.transit :as t]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [goog.dom :as gdom]))

(def app-state
  (atom {:app/title "Garment Shop"
         :app/description "Welcome to my clothes shop. Please buy something."
         :garments/by-id {}}))

(defui GarmentView
  Object
  (render [this]
          (let [p (om/props this)]
            (dom/tr nil
                    (dom/td nil (:name p))
                    (dom/td nil (:colour p))
                    (dom/td nil (:description p))))))

(def garment (om/factory GarmentView {:keyfn :name}))

(defui GarmentsView
  static om/IQuery
  (query [this] {:garments/by-id [:name :colour :description]})
  Object
  (render [this]
          (dom/div nil
                   (dom/table
                    nil
                    (dom/thead nil
                               (dom/tr nil
                                       (dom/th nil "Name")
                                       (dom/th nil "Colour")
                                       (dom/th nil "Description")))
                    (dom/tbody nil
                               (map garment (vals (:garments/by-id (om/props this))))
                     )))))

(def garments (om/factory GarmentsView))

(defui RootView
  static om/IQuery
  (query [this] [:app/title :app/description (om/get-query GarmentsView)])
  Object
  (render [this] (let [p (om/props this)]
                   (dom/div nil
                            (dom/h2 nil (:app/title p))
                            (dom/p nil (:app/description p))
                            (garments p)
                            ))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [env k params]
  (let [st @(:state env)]
    (if-let [[_ v] (find st k)]
      {:value v}
      {:value :not-found})))

(defmethod read :garments/by-id [env k params]
  (let [st @(:state env)]
    (if-let [[_ v] (find st k)]
      {:value v :remote (:ast env)}
      {:value :not-found})))

(defn send [m cb]
  (let [xhr (new js/XMLHttpRequest)]
    (.open xhr "POST" "/api")
    (.setRequestHeader xhr "Content-Type" "application/transit+json")
    (.addEventListener xhr "load"
                       (fn [evt]
                         (cb (t/read (om/reader) (.. evt -currentTarget -responseText)))))
    (.send xhr (t/write (om/writer) (:remote m)))))

(defn init []
  (enable-console-print!)

  (om/add-root! (om/reconciler {:state app-state
                                :parser (om/parser {:read read})
                                :send send
                                })
                RootView
                (.getElementById js/document "app"))

  (println "Congratulations - your environment seems to be working"))
