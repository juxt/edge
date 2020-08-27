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
   [juxt.flux.api :as flux])
  (:import
   (java.util UUID)
   (java.net URI)))

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

(defn wrap-crux-db-snapshot [h crux]
  (fn
    ([req]
     (h (assoc req :crux/db (crux/db crux))))
    ([req respond raise]
     (h (assoc req :crux/db (crux/db crux)) respond raise))))

(defn create-handler [opts]
  (let [crux (:crux opts)]
    (->
     (spin.handler/handler
      (reify
        spin.resource/ResourceLocator
        (locate-resource [_ uri {:keys [crux/db]}]

          ;; We try to locate the resource in the database.
          (if-let [e (locate-entity db uri)]
            ;; TODO: Prefer 'raw' strings in the database, but need reaped
            ;; strings for the algos. Needs some more thought.
            (update e :juxt.http/content-type memoized-content-type)

            ;; If we can't find a resource we usually return nil. However, we may
            ;; decide to still return a resource if no resource is found in the
            ;; database, because we might want to represent the case of a resource
            ;; that does not have a representation in the database, but where a PUT
            ;; might create one.

            ;; In this example, we'll to accept anything that is a file in the
            ;; /flux area.
            (when (re-matches #"/flux/[A-Za-z-]+(?:\.[A-Za-z]+)" (.getPath uri))
              {:crux.db/id (UUID/randomUUID)
               :juxt.http/uri uri})))

        spin.resource/GET
        (get-or-head [resource-provider server-provider resource response request respond raise]
          (respond
           (cond-> response
             true (update :headers conj ["content-length" (str (count (:content resource)))])
             (= (:request-method request) :get) (conj {:body (:content resource)}))))

        spin.resource/Representation
        (representation [resource-provider resource {:keys [crux/db] :as request}]
          (if (:juxt.http/variants resource)
            ;; Proactive negotiation
            (let [{:juxt.http/keys [variants varying]}
                  (pick
                   using-apache-algo
                   (conj {}
                         (decode-accept-headers request)
                         [:juxt.http/variants
                          (map #(-> (crux/entity db %)
                                    (update :juxt.http/content-type memoized-content-type))
                               (:juxt.http/variants resource))]))]
              (assoc (first variants) :juxt.http/varying varying))

            ;; No content negotiation, return the resource
            resource))

        spin.resource/LastModified
        (last-modified [_ representation]
          (:juxt.http/last-modified representation))

        spin.resource/EntityTag
        (entity-tag [_ representation]
          (:juxt.http/entity-tag representation))

        spin.resource/PUT
        (put [resource-provider content resource response request respond raise]
          (let [resource
                (into
                 resource
                 {:juxt.http/content-type (get-in request [:headers "content-type"])
                  :juxt.http/methods #{:get :put :options}
                  :content content})]
            (crux/submit-tx crux [[:crux.tx/put resource]]))))

      ;; Server capabilities
      (reify
        spin.server/ServerOptions
        (server-header [_] "JUXT MMXX Example Server")
        (server-options [_] nil)

        spin.server/RequestBody
        (request-body-as-bytes [_ request cb]
          (flux/handle-body
           request
           (fn [buffer]
             (cb (.getBytes buffer)))))))

     (wrap-crux-db-snapshot crux))))
