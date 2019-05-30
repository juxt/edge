(ns juxt.crux-ui.frontend.main
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [juxt.crux-ui.frontend.views.facade :as views]
            [clojure.core.async :as async
             :refer [take! put! <! >! timeout chan alt! go go-loop]]
            [juxt.crux-lib.async-http-client :as crux-api]))


(def example-query-str
  (with-out-str
    (cljs.pprint/pprint
      '{:full-results? true
        :find [e]
        :where [[e :name "Pablo"]]})))

(def default-db
  {:db.query/input example-query-str})

(def myc (crux-api/new-api-client "http://localhost:8080"))

#_(let [c (crux-api/new-api-client "http://localhost:8080")]
  (.then (crux-api/submitTx c [[:crux.tx/put :dbpedia.resource/Pablo-Picasso3 ; id for Kafka
   {:crux.db/id :dbpedia.resource/Pablo-Picasso3 ; id for Crux
    :name "Pablo"
    :last-name "Picasso3"}]]) #(println %))
  (.then (crux-api/q (crux-api/db c) '{:full-results? true :find [e]
         :where [[e :name "Pablo"]]}) #(println %)))

(defn post-opts [body]
  #js {:method "POST"
       :body body
       :headers #js {:Content-Type "application/edn"}})

#_(let [c (chan)
        d (chan)
        fc (chan)]
    (go (fetch "" c)))

(defn mount-root []
  (r/render [views/root] (js/document.getElementById "app")))

(defn ^:export init []
  (rf/dispatch-sync [:initialize default-db])
  (mount-root))

;; This is called every time you make a code change
(defn ^:after-load on-reload []
  (mount-root))
