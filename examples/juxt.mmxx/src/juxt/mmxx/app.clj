;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.app
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
         (if-let [e (crux/entity (crux/db crux) (java.net.URI. (.getPath uri)))]
           ;; TODO: Prefer 'raw' strings in the database, but need reaped
           ;; strings for the algos. Needs some more thought.
           (update e :juxt.http/content-type memoized-content-type)
           (do
             (println "WARN: No entity for path: " (.getPath uri))
             nil)))

       spin.resource/GET
       (get-or-head [resource-provider server-provider resource response request respond raise]
         (respond
          (cond-> response
            (= (:request-method request) :get) (conj {:body (:content resource)}))))

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
                   ]))))

       spin.resource/PUT
       (put [resource-provider server-provider resource response request respond raise]
         (respond (assoc response :body "TODO\n"))))

     (reify
       spin.server/ServerOptions
       (server-header [_] "JUXT MMXX Example Server")
       (server-options [_] nil)
       ))))
