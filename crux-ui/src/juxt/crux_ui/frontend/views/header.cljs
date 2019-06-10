(ns juxt.crux-ui.frontend.views.header
  (:require [cljss.core])
  (:require-macros [cljss.core :refer [defstyles]]))

(defstyles header-styles []
  {:display "flex"
   :justify-content "space-between"
   :align-items "center"
   :padding "16px"
   :width "100%"})

(defstyles header-logo-styles []
  {:display "flex"
   :justify-content "space-between"
   :flex "0 0 200px"
   :align-items "center"})

(defstyles header-links-styles []
  {:display "flex"
   :justify-content "space-between"
   :flex "0 0 400px"
   :align-items "center"})

(defstyles tabs-styles []
  {:display "flex"
   :justify-content "space-between"
   :font-size "20px"
   :align-items "center"})

(defstyles tabs-sep []
  {:padding "16px"})


(defn tabs []
  [:div.tabs {:class (tabs-styles)}
   [:div.tabs__item [:b "Query UI"]]
   [:div.tabs__sep {:class (tabs-sep)} "/"]
   [:div.tabs__item "Cluster"]])

(defn root []
  [:header.header {:class (header-styles)}
   [:div.header__logo {:class (header-logo-styles)}
     [:div.logo [:a {:href "/"}
                 [:img.logo-img {:width 200 :src "/static/img/console-logo.svg"}]]]]
   [:div.header__tabs
     [tabs]]
   [:div.header__links {:class (header-links-styles)}
    [:a.header__links__item {:href "https://juxt.pro/crux/docs/index.html"}
     [:span.n "Documentation"]]
    [:a.header__links__item {:href "https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux"}
     [:span.n "Community Chat"]]
    [:a.header__links__item {:href "mailto:crux@juxt.pro"}
     [:span.n "crux@juxt.pro"]]]])

