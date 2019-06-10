(ns juxt.crux-ui.frontend.views.query-ui
  (:require-macros [cljss.core])
  (:require [re-frame.core :as rf]
            [cljss.core :refer [defstyles]]
            [garden.core :as garden]
            [juxt.crux-ui.frontend.views.cluster-health :as cluster-health]
            [juxt.crux-ui.frontend.views.codemirror :as cm]
            [juxt.crux-ui.frontend.views.query-form :as q-form]
            [juxt.crux-ui.frontend.subs :as sub]))

(def ^:private -sub-query-info (rf/subscribe [:subs.query/info]))
(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-query-err (rf/subscribe [:subs.query/error]))
(def ^:private -sub-results-table (rf/subscribe [:subs.query/results-table]))


(defn query-output []
  (let [raw @-sub-query-res
        fmt (with-out-str (cljs.pprint/pprint raw))]
    [:pre.q-output.edn fmt]))

(defn query-table []
  (let [{:keys [headers rows]} @-sub-results-table]
    [:table
     [:thead
      [:tr
       (for [h headers]
         [:th h])]]
     [:tbody
      (for [r rows]
        [:tr
         (for [c r]
           [:td c])])]]))


(def query-controls-styles
  (garden/css
    [:.query-controls
      {:display :flex
       :justify-content :space-between}]))

(defn query-controls []
  [:div.query-controls
    [:style query-controls-styles]
    [:div.query-controls__item
      [:div "Select Node"]
      [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]]
    [:div.query-controls__item
      [:div "Transaction Time (optional)"]
      [:input {:type "datetime-local" :name "vt"}]] ;(.toISOString (js/Date.))
    [:div.query-controls__item
      [:div "Valid Time (optional)"]
      [:input {:type "datetime-local" :name "tt"}]]])


(def query-ui-styles
  (garden/css
    [:.query-ui
      {:font-size "16px"
       :max-width "900px"
       :margin "0 auto"}
      [:&__controls
        {:padding "16px 0"}]]))


(defn query-ui []
  [:div.query-ui
   [:style query-ui-styles]
   [:h2.query-ui__title "Query UI"]
   [:div.query-ui__controls
    [query-controls]]
   [:div.query-ui__output
    [query-output]
    [query-table]]
   [:div.query-ui__form
    [q-form/root]]])


(defn query [r]
  (let [q []
               ;conformed-query (s/conform :crux.query/query q)
               ;query-invalid? (= :clojure.spec.alpha/invalid conformed-query)
          ;start-time (System/currentTimeMillis)
          ;result (when-not query-invalid?
          ;         (api/q db q))
          ;query-time (- (System/currentTimeMillis) start-time)
          invalid? false; (and query-invalid? (not (str/blank? q)))
          on-cm-change-js "cm.save();"]
    [:div.query-editor {:style {:padding "2em"}}
     [cluster-health/root]]))
