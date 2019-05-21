(ns ^:figwheel-hooks juxt.crux-ui.frontend.main
  (:require 
    [reagent.core :as r]
    [cljs.reader :as cljs.reader]
    [cljs.pprint :as cljs.pprint]
    [clojure.string :as str]
    [clojure.core.async :as async :refer [take! put! <! >! timeout chan alt! go go-loop]]
    [juxt.crux-lib.async-http-client :as crux-api]
    ))

(let [c (crux-api/new-api-client "http://localhost:8080")]
  (.then (crux-api/submitTx c [[:crux.tx/put :dbpedia.resource/Pablo-Picasso3 ; id for Kafka
   {:crux.db/id :dbpedia.resource/Pablo-Picasso3 ; id for Crux
    :name "Pablo"
    :last-name "Picasso3"}]]) #(println %))
  (.then (crux-api/q (crux-api/db c) '{:full-results? true :find [e]
         :where [[e :name "Pablo"]]}) #(println %))
  )

(defn post-opts [body]
  #js {:method "POST"
       :body body
       :headers #js {:Content-Type "application/edn"}})

(defn fetch [path c & [opts]]
  (.then (.then (js/fetch (str "http://localhost:8080/" path) opts) #(.text %)) #(put! c %)))

(defn fetch2 [path c & [opts]]
  (.then (.then (js/fetch (str "http://localhost:8080/" path) opts) #(.text %)) #(do (put! c %) (async/close! c))))

(defn render [s]
  (r/render (cljs.reader/read-string s) (js/document.getElementById "app"))
  )

(defn merge-with-keep [a]
  (apply merge-with (fn [v1 v2] ((if (vector? v1) conj vector) v1 v2)) a))

(defn println2 [a]
  (println a)
  a)

(defn map-map-values-vec [f m]
  (into {} (for [[k vs] m] [k (mapv f vs)])))

(defn map-map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn ks-vs-to-map [ks vs]
  (merge-with-keep (map #(zipmap ks %) vs)))

(defn positions
  [pred coll]
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))

(defn qsort [f sb]
  (apply juxt (map (fn [s] (fn [x] (nth x (first (positions #{s} f))))) sb))
  )

(defn format-date [d]
  (.toISOString (new js/Date d)))

(defn decode-html [html]
  (let [txt (.createElement js/document "textarea")]
    (set! (.-innerHTML txt) html)
    (.. txt -value)))

(defn strreplace [s]
  (clojure.string/replace s #"(?:\\r\\n|\\r|\\n)" "<br />"))
;/(?:\r\n|\r|\n)/g

(defn e0? [v]
  (= 'e (first v)))

(defn renderq []
  (let [q []
               ;conformed-query (s/conform :crux.query/query q)
               ;query-invalid? (= :clojure.spec.alpha/invalid conformed-query)
          ;start-time (System/currentTimeMillis)
          ;result (when-not query-invalid?
          ;         (api/q db q))
          ;query-time (- (System/currentTimeMillis) start-time)
          invalid? false; (and query-invalid? (not (str/blank? q)))
          grow-textarea-oninput-js "this.style.height = ''; this.style.height = this.scrollHeight + 'px';"
          ctrl-enter-to-submit-onkeydown-js "window.event.ctrlKey && window.event.keyCode == 13 && document.getElementById('query-editor').form.submit();"
          on-cm-change-js "cm.save();"]
    [:div.query-editor {:style {:padding "2em"}}
     [:h2 "Cluster Health"]
     [:div.cg {:style {:display "grid" 
     :margin-right "auto" :margin-left "auto" :grid-template-columns "2em auto auto auto" :grid-row-gap "1em"
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
       )))]

     [:div {:style {:height "1em"}}]
     [:a "Refresh All"]
     [:div {:style {:height "4em"}}]
     [:h2 "Query Editor"]
           [:form.submit-query {:action "/query" :method "GET" :title "Submit with Ctrl-Enter"}
            [:fieldset
             [:div "Select Node"]
             [:select {:type "dropdown"} [:option "http://node-1.crux.cloud:8080"]]
     [:div {:style {:height "1em"}}]
             [:div "Transaction Time (optional)"]
             [:input {:type "datetime-local" :name "vt"}] ;(.toISOString (js/Date.))
     [:div {:style {:height "1em"}}]
             [:div "Valid Time (optional)"]
             [:input {:type "datetime-local" :name "tt"}]
     [:div {:style {:height "1em"}}]
             [(if invalid?
                :textarea#query-editor.invalid
                :textarea#query-editor)
              {:style {:display "block" :width "70vw"}
               :name "q" :required true :placeholder "Query"
               :rows 10;(inc (count (str/split-lines (str q))))
;               :onInput (.grow-textarea-oninput-js js/window)
;               :onKeyDown (.ctrl-enter-to-submit-onkeydown-js js/window)}
}
              (when (seq q)
                (str q))]
             (comment when invalid?
               [:div.invalid-query-message
                [:pre.edn (with-out-str
                            (pp/pprint (s/explain-data :crux.query/query q)))]])]
     [:div {:style {:height "1em"}}]
            [:input.primary {:type "submit" :value "RUN QUERY"}]]]
    ))


(defn run []
  (let [c (chan)
        d (chan)
        fc (chan)
        ]
  (go
    (fetch "" c)
    ;(render (<! c))
    (r/render (renderq) (js/document.getElementById "app"))
    

    (js/setTimeout #(js/eval (str "var opts = {lineNumbers: true, autofocus: true, mode: 'clojure', theme: 'eclipse', autoCloseBrackets: true, matchBrackets: true};
                                  var cm = CodeMirror.fromTextArea(document.getElementById('query-editor'), opts);
                                  cm.on('change', function() { cm.save(); });
                                  cm.setSize('100%%', 'auto');"
                                  )) 100)
    ;(fetch "query" d (post-opts  "{:query {:find [t e] :where [[e :design/template \"sidebar-master\"][e :message-post/design t]]}}"))

    (comment let [cr (cljs.reader/read-string (<! d))
          cc (str (ffirst (into [] cr)))]
;      (println cr)
      (render-component cc "app")
    )
    )
  ))

;; This is called once
(defonce init
  (do (set! (.-innerHTML (js/document.getElementById "app"))
            "works")
      (run)
      true))

;; This is called every time you make a code change
(defn ^:after-load reload []
  (run))
