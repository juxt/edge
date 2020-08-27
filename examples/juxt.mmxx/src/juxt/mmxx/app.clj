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
   [juxt.reap.alpha.decoders :refer [content-type]]
   [juxt.flux.api :as flux]))

(def memoized-content-type
  (memoize
   (fn [arg]
     (content-type arg))))

(defn locate-entity [db uri]
  (first
   (first
    (crux/q db {:find ['?e]
                :args [{'?uri uri}]
                :where [['?e :juxt.http/uri '?uri]]
                :full-results? true}))))

(defn create-handler [opts]
  (let [crux (:crux opts)]
    (spin.handler/handler
     (reify
       spin.resource/ResourceLocator
       (locate-resource [_ uri]

         ;; We try to locate the resource in the database.
         (if-let [e (locate-entity (crux/db crux) uri)]
           ;; TODO: Prefer 'raw' strings in the database, but need reaped
           ;; strings for the algos. Needs some more thought.
           (update e :juxt.http/content-type memoized-content-type)

           ;; If we can't find a resource we usually return nil. However, we may
           ;; decide to still return a resource if no resource is found in the
           ;; database, because we might want to represent the case of a resource
           ;; that does not have a representation in the database, but where a PUT
           ;; might create one.
           (when (re-matches #"/documents/[A-Za-z-]+(?:\.[A-Za-z]+)" (.getPath uri))
             {:crux.db/id (java.net.URI. (.getPath uri))})))

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
       (put [resource-provider content resource response request respond raise]
         (let [resource
               (into
                resource
                {:juxt.http/content-type (get-in request [:headers "content-type"])
                 :juxt.http/methods #{:get :put :options}
                 :content content})]
           (crux/submit-tx crux [[:crux.tx/put resource]]))))

     (reify
       spin.server/ServerOptions
       (server-header [_] "JUXT MMXX Example Server")
       (server-options [_] nil)

       spin.server/RequestBody
       (request-body-as-bytes [_ request cb]
         (flux/handle-body
          request
          (fn [buffer]
            (cb (.getBytes buffer)))))))))
