(ns juxt.crux-ui.frontend.views.query-editor
  (:require [juxt.crux-ui.frontend.views.codemirror :as cm]
            [re-frame.core :as rf]))

(def ^:private -sub-query-input (rf/subscribe [:subs.query/input]))

(defn- on-qe-change [v]
  (rf/dispatch [:evt.ui/query-change v]))

(defn root []
  (let [invalid? false]
    [:div.query-editor
      [cm/code-mirror
       @-sub-query-input
       {:on-change on-qe-change}]
      (if invalid?
        [:div.query-editor__err
         [:pre.edn #_(with-out-str (pp/pprint (s/explain-data :crux.query/query q)))]])]))
