 (ns edge.main
   (:require [om.next :as om :refer-macros [defui]]
             [om.dom :as dom]
             [cognitect.transit :as t]
             [goog.dom :as gdom])
   (:import [goog.net XhrIo]))

 (enable-console-print!)

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (.send XhrIo url
           (fn [e]
             (this-as this
               (cb (t/read (om/reader) (.getResponseText this)))))
           "POST" (t/write (om/writer) remote)
           #js {"Content-Type" "application/transit+json"})))

 (defonce app-state (atom {:person-list [{:db/id   1
                                          :persons [{:db/id 1 :person/name "Fred"}
                                                    {:db/id 2 :person/name "Wilma"}]}
                                         {:db/id   2
                                          :persons [{:db/id 4 :person/name "Shaggy"}]}]}))

 (defmulti read om/dispatch)
 (defmulti mutate om/dispatch)

 (def parser (om/parser {:read read
                         :mutate mutate}))

 (defmethod mutate 'person/create
   [{:keys [state]} _ _]
   {:remote true
    :action (fn []
              (let [person-ident [:person/by-id -3]]
                (swap! state
                       (fn [st]
                         (-> st
                             (update-in [:person-list/by-id 1 :persons] conj person-ident)
                             (update-in [:person/by-id] assoc -3 {:person/name "Barney"
                                                                  :db/id       -3}))))))})

 (defmethod read :default
   [{:keys [state query]} k _]
   {:value
    (let [st @state]
      (om/db->tree query (get st k) st))})

 (defmethod read :person-list/by-id
   [{:keys [state query query-root]} _ _]
   (let [st @state]
     {:value (om/db->tree query (get-in st query-root) st)}))

 (defonce reconciler (om/reconciler
                       {:state app-state
                        :normalize true
                        :parser parser
                        :send (transit-post "/api")
                        :id-key :db/id}))

(defui Person
  static om/Ident
  (ident [_ {:keys [db/id]}]
    [:person/by-id id])
  static om/IQuery
  (query [_]
    '[:person/name :db/id])
  Object
  (render [this]
    (let [{:keys [person/name db/id]} (om/props this)]
      (dom/li #js {}
              name))))

(def person (om/factory Person {:key-fn :db/id}))

(defui PersonList
   static om/Ident
   (ident [_ {:keys [db/id]}]
     [:person-list/by-id id])
   static om/IQuery
   (query [_]
     `[:db/id {:persons ~(om/get-query Person)}])
   Object
   (render [this]
     (let [{:keys [person-list] :as props} (om/props this)]
       (apply dom/ul nil
              (map person (:persons props))))))

 (def person-list (om/factory PersonList))

(defui RootView
  static om/IQueryParams
  (params [_]
    {:person-list-id 1})
  static om/IQuery
  (query [_]
    `[{:person-list ~(om/get-query PersonList)}
      {[:person-list/by-id ~'?person-list-id] ~(om/get-query PersonList)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [person-list-id]} (om/get-params this)]
      (dom/div #js {:className "main"}
        (person-list (get props [:person-list/by-id person-list-id]))
        (dom/div nil
          (dom/button #js {:onClick (fn [e]
                                      (om/transact! this
                                                    '[(person/create)]))} "Create")
          (dom/button #js {:onClick (fn [e]
                                      (om/set-query! this {:params {:person-list-id
                                                                    (cond (= 2 person-list-id)
                                                                          1
                                                                          :else
                                                                          2)}}))} "Change list"))))))

(defn init []
  (om/add-root! reconciler RootView
               (gdom/getElement "app")))

