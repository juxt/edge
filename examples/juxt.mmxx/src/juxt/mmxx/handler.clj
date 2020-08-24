;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.handler)

(defn handler [_ respond _]
  (respond
   {:status 200 :body "Hello World!"}))
