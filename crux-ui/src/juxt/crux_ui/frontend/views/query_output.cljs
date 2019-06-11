(ns juxt.crux-ui.frontend.views.query-output
  (:require [juxt.crux-ui.frontend.views.query-results-tree :as q-results-tree]
            [juxt.crux-ui.frontend.views.query-results-table :as q-results-table]
            [re-frame.core :as rf]))


(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-output-tab (rf/subscribe [:subs.ui/output-tab]))

(defn query-output-edn []
  (let [raw @-sub-query-res
        fmt (with-out-str (cljs.pprint/pprint raw))]
    [:pre.q-output-edn.edn fmt]))

(defn root []
  [:div.q-output
   (case @-sub-output-tab
     :db.ui.output-tab/table [q-results-table/root]
     :db.ui.output-tab/edn   [query-output-edn]
     :db.ui.output-tab/tree  [q-results-tree/root]
     [query-output-edn])])
