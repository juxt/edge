(ns juxt.crux-ui.frontend.views.query-ui
  (:require-macros [cljss.core])
  (:require [re-frame.core :as rf]
            [garden.core :as garden]
            [juxt.crux-ui.frontend.views.cluster-health :as cluster-health]
            [juxt.crux-ui.frontend.views.query-form :as q-form]
            [juxt.crux-ui.frontend.views.query-results-table :as q-results-table]
            [juxt.crux-ui.frontend.views.query-results-tree :as q-results-tree]
            [juxt.crux-ui.frontend.subs :as sub]))

(def ^:private -sub-query-info (rf/subscribe [:subs.query/info]))
(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-query-err (rf/subscribe [:subs.query/error]))


(defn query-output []
  (let [raw @-sub-query-res
        fmt (with-out-str (cljs.pprint/pprint raw))]
    [:pre.q-output.edn fmt]))


(def query-controls-styles
  (garden/css
    [:.query-controls
      {:display :flex
       :justify-content :space-between}
      [:&__item>label
       {:letter-spacing :.04em}]]))

(defn query-controls []
  [:div.query-controls
    [:style query-controls-styles]
    [:div.query-controls__item
      [:label "Select Node"]
      [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]]
    [:div.query-controls__item
      [:label "Transaction Time (optional)"]
      [:input {:type "datetime-local" :name "vt"}]] ;(.toISOString (js/Date.))
    [:div.query-controls__item
      [:label "Valid Time (optional)"]
      [:input {:type "datetime-local" :name "tt"}]]])


(def query-ui-styles
  (garden/css
    [:.query-ui
      {:font-size :16px
       :margin "0 auto"
      ;:border "1px solid blue"
       :max-width :900px
       :width :100%
       :height :100%
      ;:max-height :100%
       :display :grid
       :place-items :stretch
       :grid-template
       "'title' 72px
       'controls' 80px
       'output' 1fr
       'form' 300px
       'bpad' 8px"}
      [:&__title
        {:padding "8px 0"
         :grid-area :title
        ;:border "1px solid red"
         }]
      [:&__controls
        {:padding "16px 0"
         :grid-area :controls
        ;:border "1px solid orange"
         }]
      [:&__output
        {:padding "16px 0"
         :grid-area :output
        ;:border "1px solid blue"
         }]
      [:&__form
        {:padding "0px 0"
         :grid-area :form
        ;:border "1px solid green"
         }]]))


(defn query-ui []
  [:div.query-ui
   [:style query-ui-styles]
   [:h2.query-ui__title "Query Sandbox"]
   [:div.query-ui__controls
    [query-controls]]
   [:div.query-ui__output
    #_[query-output]
    #_[q-results-tree/root]
    [q-results-table/root]]
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
