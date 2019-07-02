(ns gig.swars.frontend.sidebar
  "Well, this ns is on hold for a while, due to some upcoming changes."
  (:require [gig.swars.frontend.api :as api]
            [gig.swars.frontend.state :as state]
            [reagent.core :as r]))

(defn menu []
  [:div.column.is-2
   [:aside.menu.box
    [:h2.title.is-4 "Star Wars API"]
    [:p.menu-label.title "Categories"]
    [:ul.menu-list
     (for [category (keys @state/categories)]
       (let [cat (clojure.string/capitalize (name category))]
         ^{:key category}
         [:li [:a {:on-click #(js/console.log "Hello")} cat]]))]]])

(defn table []
  (let [{c :count
         n :next
         p :previous
         r :results} @state/data]
    [:div.column
     [:table.table.is-narrow.box
      [:thead
       [:tr
        (for [th (keys (first r))]
          ^{:key th}
          [:th th])]]
      [:tbody
       (for [row r]
         ^{:key (random-uuid)}
         [:tr
          (for [td (vals row)]
            ^{:key (random-uuid)}
            [:td (if (seq td) (str td) 1)])])]]]))

(defn spa []
  [:section.section
   [:div.columns
    [menu]
    [table]]])

;; (:count :next :previous :results)
