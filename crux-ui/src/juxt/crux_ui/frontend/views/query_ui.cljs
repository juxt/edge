(ns juxt.crux-ui.frontend.views.query-ui
  (:require-macros [cljss.core])
  (:require [re-frame.core :as rf]
            [cljss.core :refer [defstyles]]
            [juxt.crux-ui.frontend.views.codemirror :as cm]
            [juxt.crux-ui.frontend.subs :as sub]))

(def ^:private -sub-query-input (rf/subscribe [:subs.query/input]))
(def ^:private -sub-query-res (rf/subscribe [:subs.query/result]))
(def ^:private -sub-query-err (rf/subscribe [:subs.query/error]))

(defn cluster-health []
  [:div.cluster-health
   [:h2 "Cluster Health"]
   [:div.cg {:style {:display "grid"
                     :margin-right "auto"
                     :margin-left "auto"
                     :grid-template-columns "2em auto auto auto"
                     :grid-row-gap "1em"
                     }}
    (let [protonode {:crux.version/version "19.04-1.0.1-alpha", :crux.kv/size 792875, :crux.index/index-version 4, :crux.tx-log/consumer-state {:crux.tx/event-log {:lag 0, :next-offset 1592957991111682, :time #inst "2019-04-18T21:30:38.195-00:00"}}}
          protonode2 {:crux.version/version "19.04-1.0.0-alpha", :crux.kv/size 792875, :crux.index/index-version 4, :crux.tx-log/consumer-state {:crux.tx/event-log {:lag 0, :next-offset 1592957988161759, :time #inst "2019-04-18T21:15:12.561-00:00"}}}
          mycluster [["http://node-1.crux.cloud:8080" [:span "Status: OK " [:span {:style {:font-weight "bold" :color "green"}} "✓"]] protonode]
                     ["http://node-2.crux.cloud:8080" [:span "Status: OK " [:span {:style {:font-weight "bold" :color "green"}} "✓"]] protonode]
                     ["http://node-3.crux.cloud:8080" [:span "Error: " [:span {:style {:font-weight "normal" :color "red"}} "Connection Timeout ✘"]] protonode2]]]
      (mapcat identity
              (for [n mycluster]
                [
                 [:div {:key (str n 0)} [:a {:style {:text-decoration "none" :font-weight "bold" :font-size "1.3em"}} "↻"]]
                 [:div {:key (str n 1)}[:a (nth n 0)]]
                 [:div {:key (str n 2)}(nth n 1)]
                 [:div {:key (str n 3)}[:pre (with-out-str (cljs.pprint/pprint (nth n 2)))]]
                 ]
                )))

    [:div {:style {:height "1em"}}]
    [:a "Refresh All"]]])

(defn on-qe-change [v]
  (rf/dispatch [:evt.ui/query-change v]))

(defn query-editor []
  (let [invalid? false]
    [:div.query-editor
      [cm/code-mirror
       @-sub-query-input
       {:on-change on-qe-change}]
      (if invalid?
        [:div.query-editor__err
         [:pre.edn #_(with-out-str (pp/pprint (s/explain-data :crux.query/query q)))]])]))

(defn- on-submit [e]
  (rf/dispatch [:evt.ui/query-submit]))

(defn query-output []
  (let [raw @-sub-query-res
        fmt (with-out-str (cljs.pprint/pprint raw))]
    [:pre.q-output.edn fmt]))

(defstyles query-ui-styles [n]
  {:font-size "16px"
   :max-width "900px"
   :margin "0 auto"})

(defn query-ui []
  [:div.query-ui {:class (query-ui-styles 1)}
   [:h2.query-ui__title "Query UI"]
   [:div.query-ui__form {:action "/query" :method "GET" :title "Submit with Ctrl-Enter"}
    [:div "Select Node"]
    [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]

    [:div "Transaction Time (optional)"]
    [:input {:type "datetime-local" :name "vt"}] ;(.toISOString (js/Date.))

    [:div "Valid Time (optional)"]
    [:input {:type "datetime-local" :name "tt"}]

    [:div.query-ui__editor
      [query-editor]]

    [:div {:style {:height "1em"}}]
    [:button.btn.btn--primary {:type "submit" :on-click on-submit} "Run Query"]]
   [:div.query-ui__output
     [query-output]]
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
     [cluster-health]]))
