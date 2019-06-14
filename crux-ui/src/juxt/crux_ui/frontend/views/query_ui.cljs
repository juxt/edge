(ns juxt.crux-ui.frontend.views.query-ui
  (:require-macros [cljss.core])
  (:require [re-frame.core :as rf]
            [garden.core :as garden]
            [juxt.crux-ui.frontend.views.cluster-health :as cluster-health]
            [juxt.crux-ui.frontend.views.query-form :as q-form]
            [juxt.crux-ui.frontend.views.query-output :as q-output]
            [juxt.crux-ui.frontend.subs :as sub]))


(def query-controls-styles
  (garden/css
    [:.query-controls
      {:display :flex
       :flex-direction :column
       :justify-content :space-between}
      [:&__item>label
       {:letter-spacing :.04em}]]))

(defn query-controls []
  [:div.query-controls
    [:style query-controls-styles]
    #_[:div.query-controls__item
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
       :max-width :1600px
       :width :100%
       :height :100%
      ;:max-height :100%
       :display :grid
       :place-items :stretch

       :grid-template
       "'title title' 72px
       'output output' 1fr
       'controls form' 330px
       'bpad bpad' 8px
        / minmax(200px, 300px) 1fr"}

      [:&__title
        {:padding "8px 0"
         :grid-area :title
        ;:border "1px solid red"
         }]
      [:&__output
        {:padding "0px 0"
         :grid-area :output
        ;:border "1px solid blue"
         }]
      [:&__controls
        {:padding "16px 0"
         :grid-area :controls
        ;:border "1px solid orange"
         }]
      [:&__form
        {:padding "0px 0"
         :grid-area :form
       ; :border "1px solid green"
         }]]))


(defn query-ui []
  [:div.query-ui
   [:style query-ui-styles]
   [:h2.query-ui__title "Query Sandbox"]
   [:div.query-ui__output
    [q-output/root]]
   [:div.query-ui__controls
    [query-controls]]
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
