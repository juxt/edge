;; Copyright © 2016, JUXT LTD.

;; Here's a simple single page app written in ClojureScript that uses
;; Reagent.

(ns edge.phonebook-app.main
  (:require
   [accountant.core :as accountant]
   [bidi.bidi :as bidi]
   [reagent.core :as r]
   [cljs.reader :refer [read-string]]
   [edge.phonebook-app.client-routes :refer [client-routes]]))

(def app-state (r/atom {}))

(defn get-phonebook-data []
  (println "Loading phonebook data")
  (doto
      (new js/XMLHttpRequest)
      (.open "GET" "/phonebook")
      (.setRequestHeader "Accept" "application/edn")
      (.addEventListener
       "load"
       (fn [evt]
         (swap! app-state
                assoc :phonebook
                (read-string evt.currentTarget.responseText))))
      (.send)))

(defn modified-entry [state id]
  (merge (get (:phonebook state) id)
                     (:staging state)))

(defn save-entry [state]
  (let [id (:current state)
        entry (modified-entry state id)]
    (doto
        (new js/XMLHttpRequest)
        (.open "PUT" (str "/phonebook/" id))
        (.setRequestHeader "Content-Type" "application/edn")
        (.addEventListener
         "load"
         (fn [e]
           (when (<= 200 e.target.status 299)
             (swap! app-state update :phonebook conj [id entry]))))
        (.send (pr-str entry)))))

(defn delete-entry [state]
  (let [[id entry] (:current state)]
    (println "Deleting entry:" id entry)
    (doto
        (new js/XMLHttpRequest)
        (.open "DELETE" (str "/phonebook/" id))
        (.addEventListener
         "load"
         (fn [e]
           (when (= e.target.status 200)
             (swap! app-state update :phonebook dissoc id)
             (swap! app-state dissoc :current))))
        (.send))))

(defn needs-saving? [state]
  (when-let [db-entry (get (:phonebook state)
                           (:current state))]
    (not= (:staging state) db-entry)))

(defn changer [path]
  (fn [ev]
    (swap! app-state assoc-in path (.-value (.-target ev)))))

(defn phonebook []
  (fn []
    (let [state @app-state]
      [:div
       [:p "Select any of the entries in this table to reveal a form below."]
       [:table
        [:thead
         [:tr {;; :on-click (fn [_] (swap! app-state dissoc :current))
               }
          [:th "Id"]
          [:th "Firstname"]
          [:th "Surname"]
          [:th "Phone"]]]
        [:tbody
         (for [[id {:keys [firstname surname phone]}] (:phonebook state)]
           ^{:key (keyword "index" id)}
           [:tr {;;:on-click (fn [_] (swap! app-state assoc :current [id (get (:phonebook state) id)]))
                 :class (if (= id (-> state :current)) "selected" "")
                 }
            [:td [:a {:href (str "/phonebook-app/" id)} id]]
            [:td firstname]
            [:td surname]
            [:td phone]]
           )]]

       (when-let [id (:current state)]
         (let [entry (modified-entry state id)]
           [:div.container
            [:h1 id]
            [:h3 (:firstname entry) " " (:surname entry)]
            [:form
             {:on-submit (fn [ev] (.preventDefault ev))}
             [:p
              [:label "Id"]
              [:input {:type "text"
                       :disabled true
                       :value id}]]
             (for [[k label] [[:firstname "Firstname"]
                              [:surname "Surname"]
                              [:phone "Phone"]]]
               ^{:key (keyword "field" k)}
               [:p
                [:label label]
                [:input {:type "text"
                         :value (get entry k)
                         :on-change (changer [:staging k])}]])

             [:button {:disabled (not (needs-saving? state))
                       :on-click (fn [ev] (save-entry state))} "Save"]

             [:button {:on-click (fn [ev] (delete-entry state))} "Delete"]]]))])))

;; Navigation

(def routes
  ["/phonebook-app"
   client-routes])

(defn init-route
  [{:keys [handler route-params] :as route}]
  (println "init-route:" route)
  (case handler
    :index (swap! app-state dissoc :current)
    :entry (let [{:keys [id]} route-params]
             (let [id (read-string id)]
               (swap! app-state
                      (fn [old-state]
                        (assoc old-state
                               :current id
                               :staging (get-in old-state [:phonebook id]))))))))


(defn path-exists?
  [route]
  (boolean (bidi/match-route routes route)))

(defn init []
  (enable-console-print!)

  (when-let [section (. js/document (getElementById "phonebook"))]
    (get-phonebook-data)
    (accountant/configure-navigation!
     {:nav-handler #(init-route (bidi/match-route routes %))
      :path-exists? path-exists?})
    (accountant/dispatch-current!)
    (r/render-component [phonebook] section)
    (println "Phonebook app initialized")))

(defonce run (init))
