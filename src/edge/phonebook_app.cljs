;; Copyright © 2016, JUXT LTD.

;; Here's a simple single page app written in ClojureScript that uses
;; Reagent.

(ns edge.phonebook-app
  (:require
   [reagent.core :as r]
   [cljs.reader :refer [read-string]]))

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

(defn save-entry [state]
  (let [[id entry] (:current state)]
    (println "Saving entry:" id entry)
    (doto
        (new js/XMLHttpRequest)
        (.open "PUT" (str "/phonebook/" id))
        (.setRequestHeader "Content-Type" "application/edn")
        (.addEventListener
         "load"
         (fn [e]
           (when (= e.target.status 200)
             (swap! app-state update :phonebook conj (:current state)))))
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
                           (first (:current state)))]
    (not= (second (:current state)) db-entry)))

(defn changer [path]
  (fn [ev]
    (swap! app-state assoc-in path (.-value (.-target ev)))))

(defn phonebook []
  (fn []
    (let [state @app-state]
      [:div
       [:p "Select any of the entries in this table to reveal a form below."]
       [:p
        [:table
         [:thead
          [:tr {:on-click (fn [_] (swap! app-state dissoc :current))}
           [:th "Id"]
           [:th "Firstname"]
           [:th "Surname"]
           [:th "Phone"]]]
         [:tbody
          (for [[id {:keys [firstname surname phone]}] (:phonebook state)]
            ^{:key (keyword "index" id)}
            [:tr {:on-click (fn [_] (swap! app-state assoc :current [id (get (:phonebook state) id)]))
                  :class (if (= id (-> state :current first)) "selected" "")
                  }
             [:td id]
             [:td firstname]
             [:td surname]
             [:td phone]]
            )]]]

       (when-let [[id entry] (:current state)]
         [:div.container
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
                       :on-change (changer [:current 1 k])}]])

           [:button {:disabled (not (needs-saving? state))
                     :on-click (fn [ev] (save-entry state))} "Save"]

           [:button {:on-click (fn [ev] (delete-entry state))} "Delete"]]])])))

(defn init [section]
  (get-phonebook-data)
  (r/render-component [phonebook] section))
