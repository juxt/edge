(ns ^:figwheel-hooks juxt.crux-ui.frontend.main
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs.reader :as cljs.reader]
            [cljs.pprint :as cljs.pprint]
            [clojure.string :as str]
            [clojure.core.async :as async
             :refer [take! put! <! >! timeout chan alt! go go-loop]]
            [juxt.crux-lib.async-http-client :as crux-api]))

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))

(defonce do-timer (js/setInterval dispatch-timer-event 1000))


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :time
  (fn [db _]     ;; db is current app state. 2nd unused param is query vector
    (:time db))) ;; return a query computation over the application state

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))


; this in reagent (for [page pages]


(def myc (crux-api/new-api-client "http://localhost:8080"))

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


(defn init-code-mirror []
  (let [opts #js {:lineNumbers true
                   autofocus: true
                   mode: 'clojure'
                   theme: 'eclipse'
                   autoCloseBrackets: true
                   matchBrackets: true}
        cm (CodeMirror/fromTextArea "#query-editor" opts)]
   ;cm.on('change', function() { cm.save(); });
   ;cm.setSize('100%%', 'auto');"
   ))


(defn run []
  (let [c (chan)
        d (chan)
        fc (chan)
        ]
    (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (go
    (fetch "" c)
    ;(render (<! c))
    (r/render [ui] (js/document.getElementById "app"))


    (js/setTimeout init-code-mirror 100)
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
