(ns gig.swars.frontend.components
  (:require [gig.swars.frontend.api :as api]
            [gig.swars.frontend.state :as state]
            [reagent.core :as r]
            [clojure.string :refer [capitalize split join includes?]]))

(defn str-to-date [s]
  "Creates a Date object out of a string, and generates Date-Month-Year string"
  (->> (js/Date. s)
       ((juxt #(.getDate %) #(.getMonth %) #(.getFullYear %)))
       (join "-")))

(defn process-td [[k v]]
  "I'm not proud of this function, but it does the job.
   What job? - Well, the major problem this procedure solves is processing data which contains links
   and building up <a> element out of it."
  (let [create-el (fn [l] ^{:key (random-uuid)}
                    [:span " "
                     [:a {:href l} (last (split l "/"))]])]
    (cond
      (#{:created :edited} k) (str-to-date v)
      (vector? v) (for [link v] (create-el link))
      (clojure.string/includes? (str v) "http") (create-el v)
      :else (str v))))

(defn by-id [id]
  (.getElementById js/document id))

(defn activate [c]
  "Upon changing the category we need to change the selected element as well."
  (aset (by-id "category") "innerText" c))

(defn navbar-menu []
  "A navigation bar drop-down menu, populated by the categories of the film data."
  (let [cats (map name (keys @state/categories))]
    [:div.navbar-start
     [:div.navbar-item.has-dropdown.is-hoverable
      [:a.navbar-link {:id "category"} (first cats)]
      [:div.navbar-dropdown
       (for [c cats]
         ^{:key c}
         [:div.navbar-item
          [:a
           {:on-click #((juxt api/fetch-data activate) c)}
           c]])]]]))

(defn navbar-title []
  "A root with text and /"
  [:div.navbar-end
   [:div.navbar-item [:a {:href "/"} "Star Wars API Browser"]]])

(defn navbar []
  "Navigation bar column, puts together menu and a title."
  [:div.column
   [:nav.navbar.is-light
    [navbar-menu]
    [navbar-title]]])

(defn table []
  "the title is self-descriptive, results is being pulled from data r/atom
   and table populated with that very data including headers (thead)."
  (let [{r :results} @state/data]
    [:div.column
     [:table.table.is-narrow.box.is-hoverable
      [:thead
       [:tr
        (for [th (map name (keys (first r)))]
          ^{:key th}
          [:th (join " " (map capitalize (split th #"_")))])]]
      [:tbody
       (for [row r]
         ^{:key (random-uuid)}
         [:tr
          (for [td row]
            ^{:key (random-uuid)}
            [:td (process-td td)])])]]]))

(defn bottom []
  "Here comes controller buttons and some useful info such as number of total records and a current range"
  (let [{c :count
         n :next
         p :previous} @state/data
        f #(if % false true)
        p? (f p)
        n? (f n)
        start-value (if p (* 10 (js/parseInt (last p))) 0)
        end-value (if n (* 10 (dec (js/parseInt (last n)))) c)] 
    [:div.column
     [:div.tags.is-pulled-left
      [:span.tag.is-light.is-medium "Total in the Category: " c]
      [:span.tag.is-light.is-medium "Showing: " start-value " - "  end-value]]
     [:div.buttons.is-pulled-right
      [:a.button.is-light
       {:disabled p?
        :on-click #(api/fetch-raw p)} "Previous"]
      [:a.button.is-light
       {:disabled n?
        :on-click #(api/fetch-raw n)} "Next"]]]))

(defn page []
  "The main container for the page components with 3 columns."
  [:div.container
   [navbar]
   [table]
   [bottom]])
