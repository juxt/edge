(ns juxt.crux-ui.frontend.views.query-output
  (:require [juxt.crux-ui.frontend.views.query-results-tree :as q-results-tree]
            [juxt.crux-ui.frontend.views.query-results-table :as q-results-table]
            [garden.core :as garden]
            [re-frame.core :as rf]))


(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-output-tab (rf/subscribe [:subs.ui/output-tab]))
(def color-placeholder :grey)

(defn- query-output-edn []
  (let [raw @-sub-query-res
        fmt (with-out-str (cljs.pprint/pprint raw))]
    [:pre.q-output-edn.edn fmt]))


(def empty-placeholder
  [:div.q-output-empty "Your query or transaction results here shall be"])

(def ^:private q-output-styles
  [:style
    (garden/css
      [:.q-output
       {:border "0px solid red"
        :height :100%
        :display :grid
        :grid-template "'side main' 1fr / minmax(200px, 300px) 1fr"}
       [:&__side
        {;:background :blue
         :border "0px solid red"

         :grid-area :side}]
       [:&__main
        {;:background :grey
         :border-radius :2px
         :border "1px solid hsl(0, 0%, 85%)"
         :grid-area :main}]]
      [:.q-output-empty
       {:height :100%
        :display :flex
        :color color-placeholder
        :align-items :center
        :justify-content :center}])])

(defn root []
  [:div.q-output
   q-output-styles
   [:div.q-output__side
     [q-results-tree/root]]
   [:div.q-output__main
     (case @-sub-output-tab
       :db.ui.output-tab/table [q-results-table/root]
       :db.ui.output-tab/edn   [query-output-edn]
       :db.ui.output-tab/empty empty-placeholder
       [q-results-table/root])]])
