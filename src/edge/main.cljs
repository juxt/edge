;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [goog.dom :as gdom]))

(def app-state
  (atom {:app/title "The title"
         :app/description "Welcome to Om Next"
         :garments/by-id
         {"nct_sandbox_dress_37_7cpxa7" {:name "Dress"
                                         :colour "Yellow"
                                         :description "A nice dress"}
          "nct_sandbox_jacket_4_btcjsf" {:name "Jacket"
                                         :colour "Blue"
                                         :description "Very fetching"}
          "nct_sandbox_trousers_2_5bahk2" {:name "Trousers"
                                           :colour "Brown"
                                           :description "What's more to say?"}}}))

(defui RootView
  static om/IQuery
  (query [this] [:app/title :app/description])
  Object
  (render [this] (dom/div nil
                          (dom/h2 nil (:app/title (om/props this)))
                          (dom/p nil (:app/description (om/props this))))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [env k params]
  {:value (-> env :state deref k)}
)

(defn init []
  (enable-console-print!)

  (om/add-root! (om/reconciler {:state app-state
                                :parser (om/parser {:read read
                                                    })
                                
                                })
                RootView
                (.getElementById js/document "app"))

  (println "Congratulations - your environment seems to be working"))
