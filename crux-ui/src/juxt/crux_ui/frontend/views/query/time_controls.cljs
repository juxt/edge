(ns juxt.crux-ui.frontend.views.query.time-controls
  (:require [garden.core :as garden]
            [juxt.crux-ui.frontend.functions :as f]
            [juxt.crux-ui.frontend.logging :as log]
            [re-frame.core :as rf]))


(defn- on-time-change [time-type evt]
  (try
    (let [v (f/jsget evt "target" "value")]
      (log/log "value parsed" v)
      (rf/dispatch [:evt.ui.query/time-change time-type (js/Date. v)]))
    (catch js/Error err
      (log/error err))))


(defn- on-vt-change [evt]
  (on-time-change :crux.ui.time-type/vt evt))

(defn- on-tt-change [evt]
  (on-time-change :crux.ui.time-type/tt evt))

(def ^:private query-controls-styles
  [:style
   (garden/css
     [:.query-controls
      {:display         :flex
       :flex-direction  :column
       :justify-content :space-between
       :padding         :8px}
      [:&__item
       {:margin-bottom :16px}]
      [:&__item>label
       {:letter-spacing :.04em}]
      [:&__item>input
       {:padding       :4px
        :border-radius :2px
        :margin-top    :4px
        :border        "1px solid hsl(0, 0%, 85%)"}]])])


(defn root []
  [:div.query-controls
   query-controls-styles
   #_[:div.query-controls__item
      [:label "Select Node"]
      [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]]
   [:div.query-controls__item
    [:label "Transaction Time (optional)"]
    [:input {:type "datetime-local" :name "vt" :on-change on-vt-change}]]
   [:div.query-controls__item
    [:label "Valid Time (optional)"]
    [:input {:type "datetime-local" :name "tt" :on-change on-tt-change}]]])
