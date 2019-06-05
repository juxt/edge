(ns juxt.crux-ui.frontend.views.query-ui
  (:require-macros [cljss.core])
  (:require [re-frame.core :as rf]
            [cljss.core :refer [defstyles]]
            [juxt.crux-ui.frontend.views.cluster-health :as cluster-health]
            [juxt.crux-ui.frontend.views.codemirror :as cm]
            [juxt.crux-ui.frontend.subs :as sub]))

(def ^:private -sub-query-input (rf/subscribe [:subs.query/input]))
(def ^:private -sub-query-info (rf/subscribe [:subs.query/info]))
(def ^:private -sub-query-input-malformed (rf/subscribe [:subs.query/input-malformed?]))
(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-query-err (rf/subscribe [:subs.query/error]))
(def ^:private -sub-results-table (rf/subscribe [:subs.query/results-table]))


(defn- on-qe-change [v]
  (rf/dispatch [:evt.ui/query-change v]))

(defn- on-submit [e]
  (rf/dispatch [:evt.ui/query-submit]))

(defn query-editor []
  (let [invalid? false]
    [:div.query-editor
      [cm/code-mirror
       @-sub-query-input
       {:on-change on-qe-change}]
      (if invalid?
        [:div.query-editor__err
         [:pre.edn #_(with-out-str (pp/pprint (s/explain-data :crux.query/query q)))]])]))

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

(defstyles query-ui-styles [n]
  {:font-size "16px"
   :max-width "900px"
   :margin "0 auto"})

(defn query-ui []
  [:div.query-ui {:class (query-ui-styles 1)}
   [:h2.query-ui__title "Query UI"]
   [:div.query-ui__output
    [query-output]
    [query-table]
    ]
   [:div.query-ui__form {:action "/query" :method "GET" :title "Submit with Ctrl-Enter"}
    [:div "Select Node"]
    [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]

    [:div "Transaction Time (optional)"]
    [:input {:type "datetime-local" :name "vt"}] ;(.toISOString (js/Date.))

    [:div "Valid Time (optional)"]
    [:input {:type "datetime-local" :name "tt"}]

    [:div.query-ui__editor
      [query-editor]]
    (if-let [e @-sub-query-input-malformed]
      [:div.query-ui__editor-err
       "Query input appears to be malformed: " (.-message e)])

    [:div {:style {:height "1em"}}]
    [:button.btn.btn--primary {:type "submit" :on-click on-submit} "Run Query"]]
   ])


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
