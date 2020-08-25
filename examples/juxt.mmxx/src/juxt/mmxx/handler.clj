;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.handler
  (:require
   [crux.api :as crux]
   [juxt.spin.alpha.handler :as spin.handler]
   [juxt.spin.alpha.resource :as spin.resource]))

(defn create-handler [opts]
  (let [crux (:crux opts)]
    (spin.handler/handler
     (reify
       spin.resource/ResourceLocator
       (locate-resource [_ uri]
         (let [e (crux/entity (crux/db crux) (java.net.URI. "http://localhost:8082/"))]
           e))
       spin.resource/Resource
       (invoke-method [resource-provider server-provider resource response request respond raise]
         (respond {:status 200 :body (:content resource)})))
     (reify))))
