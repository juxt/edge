;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.handler
  (:require
   [crux.api :as crux]
   [juxt.spin.alpha.handler :as spin.handler]
   [juxt.spin.alpha.resource :as spin.resource]
   [juxt.spin.alpha.server :as spin.server]
   [juxt.pick.alpha.core :refer [pick]]
   [juxt.pick.alpha.apache :refer [using-apache-algo]]
   [juxt.reap.alpha.ring :refer [decode-accept-headers]]
   [juxt.reap.alpha.decoders :refer [content-type]]))

(def memoized-content-type
  (memoize
   (fn [arg]
     (content-type arg))))

(defn create-handler [opts]
  (let [crux (:crux opts)]
    (spin.handler/handler
     (reify

       spin.resource/ResourceLocator
       (locate-resource [_ uri]
         (let [e (crux/entity (crux/db crux) (java.net.URI. (.getPath uri)))]
           e))

       spin.resource/Resource
       (invoke-method [resource-provider server-provider resource response request respond raise]
         (let [method (:request-method request)]
           (case method
             (:head :get)
             (respond
              (cond-> response
                (= method :get) (conj {:body (:content resource)})))

             :put
             (respond (assoc response :body "TODO\n")))))

       spin.resource/ContentNegotiation
       (best-representation [resource-provider resource request]
         (let [db (crux/db crux)]
           (pick
            using-apache-algo
            (conj {}
                  (decode-accept-headers request)
                  [:juxt.http/variants
                   (map #(-> (crux/entity db %)
                             (update :juxt.http/content-type memoized-content-type))
                        (:juxt.http/variants resource))
                   ])))))

     (reify
       spin.server/ServerOptions
       (server-header [_] "JUXT MMXX Example Server")
       (server-options [_] nil)
       ))))
