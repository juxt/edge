(ns juxt.crux-ui.frontend.io.query
  (:require [clojure.core.async :as async
             :refer [take! put! <! >! timeout chan alt! go go-loop]]
            [re-frame.core :as rf]
            [juxt.crux-lib.async-http-client :as crux-api]))

(defn post-opts [body]
  #js {:method "POST"
       :body body
       :headers #js {:Content-Type "application/edn"}})

#_(let [c (chan)
        d (chan)
        fc (chan)]
    (go (fetch "" c)))

(defn submit-tx []
  (let [c (crux-api/new-api-client "http://localhost:8080")]
    (.then (crux-api/submitTx c [[:crux.tx/put :dbpedia.resource/Pablo-Picasso3 ; id for Kafka
                                  {:crux.db/id :dbpedia.resource/Pablo-Picasso3 ; id for Crux
                                   :name "Pablo"
                                   :last-name "Picasso3"}]])
           #(println %))))

(defn on-exec-success [resp]
  (rf/dispatch [:evt.io/query-success resp]))

(defn exec [query-text]
  (let [c (crux-api/new-api-client "http://localhost:8080")
        promise (crux-api/q (crux-api/db c) query-text)]
    (.then promise on-exec-success)))

(comment
  (exec (pr-str '{:full-results? true
                  :find [e]
                  :where [[e :name "Pablo"]]})))