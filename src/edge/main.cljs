;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cognitect.transit :as t]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [goog.dom :as gdom]
   [cljs.core.async :as a :refer [chan >! <! close! timeout alts! mult tap mix pub sub]]
   ))

(def ch1 (chan 10))
(def ch2 (chan 10))

(go-loop [c 1000]
  (>! ch1 (rand-int 40))
  (<! (timeout 700))
  (when (> c 0)
    (recur (dec c))))

(go-loop [c 1000]
  (>! ch2 (rand-int 20))
  (<! (timeout 1000))
  (when (> c 0)
    (recur (dec c))))


(def ch3 (chan 20 (comp
                   (filter odd?)
                   (map (fn [x] (* 2 x)))
                   (partition-all 3)
                   (map str)
                   (take 10))))


(go-loop []
  (let [[v ch] (alts! [ch1 ch2])]
    (>! ch3 v))
  (recur))

(go-loop []
  (when-let [v (<! ch3)]
    (js/console.log v)
    (recur)))

#_(go-loop []
    (let [x (alts! [ch1 ch2 #_[ch3 "Hello"]])]
      (if (vector? x)
        (js/console.log (str "") (first x))
        (js/console.log "Managed to write into channel 3")
        )
      (<! (timeout 200))
      )
    (recur))





(def app-state
  (atom {:app/title "Garment Shop"
         :app/description "Welcome to my clothes shop. Please buy something."
         :app/likes 10
         :garments/by-id {}}))

(go-loop []
  (<! (timeout 1000))
  (swap! app-state update :app/likes dec)
  (recur)
  )

(defui GarmentView
  static om/IQuery
  (query [this] [:likes])
  Object
  (render
   [this]
   (let [p (om/props this)]
     (dom/tr nil
             (dom/td nil (:name p))
             (dom/td nil (:sku p))
             (dom/td nil (:description p))
             (dom/td nil "£" (:GBP (:price p)))
             (dom/td nil (apply str (interpose "," (:sizes p))))
             (dom/td nil (:likes p))
             (dom/td nil (dom/button
                          #js {:onClick (fn [evt]
                                          (om/transact! this `[(~'likeitem {:id ~(:id p)})])
                                          )} "Like"))
             ))))



(def garment (om/factory GarmentView {:keyfn :name}))

(defui GarmentsView
  #_om/ITxIntercept
  #_(tx-intercept [_ _] (js/console.log "Transaction!!!"))
  static om/IQuery
  (query [this] {:garments/by-id [:id :name :sku :description :price :likes]})
  Object
  (render [this]
          
          [:div
           [:table {:border 1}
            [:tbody
             (for [i (range 10)]
               [:tr ])]]]
          (dom/div nil
                   (dom/table
                    nil
                    (dom/thead nil
                               (dom/tr nil
                                       (dom/th nil "Name")
                                       (dom/th nil "SKU")
                                       (dom/th nil "Description")
                                       (dom/th nil "Price")
                                       (dom/th nil "Sizes")
                                       (dom/th nil "Likes")
                                       (dom/th nil "Like")
                                       ))
                    (dom/tbody nil
                               (map garment (vals (:garments/by-id (om/props this))))
                     )))))

(def garments (om/factory GarmentsView))

(defui RootView
  static om/IQuery
  (query [this] [:app/title :app/description :app/likes (om/get-query GarmentsView)])

  Object
  (render [this]
          (let [p (om/props this)]
            (dom/div nil
                     (dom/h2 nil (:app/title p))
                     (dom/p nil (:app/description p))
                     (dom/p nil (str "Likes: "
                                     (or
                                      (:app/likes p)
                                      "NIL")
                                     "..."))
                     (dom/button
                      #js {:onClick
                           (fn [evt]
                             (js/console.dir (om/transact! this '[(like)]))
                             (js/console.log "Like!!!!"))} "Like")
                     (garments p)
                     ))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod mutate 'like [env k params]
  #_{:action (fn [] (swap! (:state env)
                           (fn [old] (update old :app/likes inc))))}

  {:value {:keys [:app/likes]}
   :action #(swap! (:state env) update :app/likes inc)})

(defmethod mutate 'likeitem [env k params]
  (js/console.log "Liking item!")
  (js/console.log (str "Params is" params))
  (let [id (:id params)]
    {:value {:keys [:likes]}
     :action #(swap! (:state env) update-in [:garments/by-id id :likes] inc)}))

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
                         (let [response (t/read (om/reader) (.. evt -currentTarget -responseText))]
                           (println "response:" response)
                           (cb response))))
    (.send xhr (t/write (om/writer) (:remote m)))))

(def reconciler (om/reconciler {:state app-state
                                :parser (om/parser
                                         {:read read
                                          :mutate mutate})
                                :send send
                                }))

(defn init []
  (enable-console-print!)

  (let [es (new js/EventSource "/newsfeed")]
    (.addEventListener es "message"
                       (fn [evt] (js/console.log (.. evt -data)))
                       )
    )

  (om/add-root! reconciler
                RootView
                (.getElementById js/document "app"))

  (println "Congratulations - your environment seems to be working"))
