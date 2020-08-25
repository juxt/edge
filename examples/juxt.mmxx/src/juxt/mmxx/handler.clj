;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.handler)

(defn create-handler [opts]
  (fn [_ respond _]
    (respond
     {:status 200 :body "Hello World!"})))
