(ns gig.swars.frontend.api
  (:require [ajax.core :refer [GET]]
            [gig.swars.frontend.state :as state]))

(defn cat-handler [response]
  (reset! state/categories response))

(defn table-handler [response]
  (reset! state/data response))

(defn error-handler [{:keys [status status-text]}]
  (println (str status " - " status-text)))

(defn fetch-raw [uri]
  (GET uri
       {:handler table-handler
        :error-handler error-handler
        :response-format :json
        :keywords? true}))

(defn fetch [handler & {:keys [category]
                        :or {category ""}}]
  (let [uri (str "https://swapi.co/api/" (name category))]
    (GET uri
         {:handler handler
          :error-handler error-handler
          :response-format :json
          :keywords? true})))

(defn fetch-categories []
  (fetch cat-handler))

(defn fetch-data [category]
  (fetch table-handler :category category))

(fetch-categories)
(fetch-data "people")
