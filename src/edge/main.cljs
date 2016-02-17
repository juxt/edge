;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cognitect.transit :as t]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [goog.dom :as gdom]
   [cljs.core.async :as a :refer [chan >! <! close! timeout alts! mult tap mix pub sub]]))

(def app-state
  (atom {:name "London"
         :bikepoints/by-id {"123" {:id "" :common-name "Loading..."}}}))

(defmulti mutate om/dispatch)

(defn read [env k params]
  (let [st @(:state env)]
    (if-let [[_ v] (find st k)]
      (merge {:value v}
             (case k
               (:bikepoints/by-id) {:remote (:ast env)}
               nil))
      {:value :not-found})))

(defmethod mutate 'inc [env k params]
  {:action #(swap! (:state env) update :counter inc)})

(defmethod mutate 'reset [env k params]
  {:action #(swap! (:state env) assoc :counter 0)})

(defmethod mutate 'message [env k params]
  {:action #(swap! (:state env) assoc :last-sse (str (:data params)))})

(defn send [m cb]
  (let [q (:remote m)
        request-body (t/write (om/writer) q)]
    (doto
        (new js/XMLHttpRequest)
      (.open "POST" "/data")
      (.setRequestHeader "Accept" "application/transit+json;q=0.9,application/transit+msgpack;q=0.8")
      (.setRequestHeader "Content-Type" "application/transit+json")
      (.addEventListener "load" (fn [evt]
                                  (let [body
                                        (.. evt -currentTarget -responseText)]
                                    (let [result (t/read (om/reader) body)]
                                      (println ">" result)
                                      (cb result)))))
      (.send request-body))))

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read
                        :mutate mutate})
    :send send}))

(defui BikepointsView
  static om/IQuery
  (query [this]
         [:name {:bikepoints/by-id [:id
                                    :common-name
                                    {:additional-properties [:nb-bikes]}]}])
  Object
  (render [this]
          (let [p (om/props this)]
            (dom/div nil
                     (dom/h1 nil "Bikepoints - " (:name p))
                     (dom/table nil 
                                (dom/thead nil
                                           (dom/tr nil
                                                   (dom/th nil "Bikepoint")
                                                   (dom/th nil "Bikes left")))
                                (dom/tbody nil
                                           (for [bike (sort-by :id (vals (:bikepoints/by-id p)))]
                                             (dom/tr nil
                                                     (dom/td nil (:common-name bike))
                                                     (dom/td nil (-> bike :additional-properties :nb-bikes)))
                                             )))
                     ))))

(defn init []
  (enable-console-print!)
  (println "Congratulations!  - your environment seems to be working!")

  (om/add-root! reconciler BikepointsView (js/document.getElementById "app"))

  )

