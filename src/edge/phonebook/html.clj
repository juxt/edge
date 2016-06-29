(ns edge.phonebook.html
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [selmer.parser :as selmer]
   [yada.yada :as yada]))

(defn index-html [ctx entries q]
  (selmer/render-file
   "phonebook.html"
   {:title "Edge phonebook"
    :ctx ctx
    :content
    (html
     [:section
      [:form#search {:method :get}
       [:input {:type :text :name :q :value q}]
       [:input {:type :submit :value "Search"}]]

      (if (not-empty entries)
        [:table
         [:thead
          [:tr
           [:th "Entry"]
           [:th "Surname"]
           [:th "Firstname"]
           [:th "Phone"]]]

         [:tbody
          (for [[id {:keys [surname firstname phone]}] entries
                :let [href (:href (yada/uri-for ctx :edge.resources/phonebook-entry {:route-params {:id id}}))]]
            [:tr
             [:td [:a {:href href} id]]
             [:td surname]
             [:td firstname]
             [:td phone]])]]
        [:h2 "No entries"])

      [:h4 "Add entry"]

      [:form {:method :post}
       [:style "label { margin: 6pt }"]
       [:p
        [:label "Surname"]
        [:input {:name "surname" :type :text}]]
       [:p
        [:label "Firstname"]
        [:input {:name "firstname" :type :text}]]
       [:p
        [:label "Phone"]
        [:input {:name "phone" :type :text}]]
       [:p
        [:input {:type :submit :value "Add entry"}]]]])})
  )
