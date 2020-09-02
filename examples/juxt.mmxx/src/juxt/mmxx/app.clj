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

(def encoder (java.util.Base64/getEncoder))
(def decoder (java.util.Base64/getDecoder))

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
            (let [e-hist (crux/entity-tx db (:crux.db/id e))]
              (cond-> e
                (:juxt.http/content-type e) (update :juxt.http/content-type memoized-content-type)
                ;; An empty byte-array signifies that a payload exists.
                (:juxt.http/base64-encoded-payload e) (assoc :juxt.http/payload (byte-array []))
                (:crux.db/valid-time e-hist) (assoc :juxt.http/last-modified (:crux.db/valid-time e-hist))
                (:crux.db/content-hash e-hist) (assoc :juxt.http/entity-tag (str "\"" (:crux.db/content-hash e-hist) "\""))))

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

        spin.resource/GET
        (get-or-head [resource-provider server-provider resource response
                      {:keys [crux/db] :as request} respond raise]
          (cond
            ;; Redirect
            (:juxt.http/redirect resource)
            (if-let [e (crux/entity db (:juxt.http/redirect resource))]
              (respond
               (-> response
                   (assoc :status 307)
                   (update :headers conj ["location" (str (:juxt.http/uri e))])))
              (raise
               (ex-info
                (format
                 "Redirect reference to %s doesn't exist" (:juxt.http/redirect resource))
                {:resource resource})))

            ;; We have content to send
            (:juxt.http/base64-encoded-payload resource)
            (let [payload-bytes (.decode decoder (:juxt.http/base64-encoded-payload resource))]
              (respond
               (cond-> response
                 true (update :headers conj ["content-length" (str (count payload-bytes))])
                 (= (:request-method request) :get) (conj {:body payload-bytes}))))

            :else
            (respond {:status 404})))

        spin.resource/PUT
        (put [resource-provider representation-in-request resource response request respond raise]

          ;; Auth check ! Are they a super-user - account owner

          (let [base64-encoded-payload
                (.encodeToString encoder (:juxt.http/payload representation-in-request))
                new-resource
                (into
                 resource
                 (concat
                  (dissoc representation-in-request :juxt.http/payload)
                  {:juxt.http/last-modified (java.util.Date.)
                   :juxt.http/entity-tag (str "\"" (hash base64-encoded-payload) "\"")
                   :juxt.http/base64-encoded-payload base64-encoded-payload
                   :juxt.http/methods #{:get :put :options}}))]

            (crux/submit-tx
             crux
             [[:crux.tx/put new-resource]])

            ;; We could respond here, or we return a new resource for the Spin to respond
            ;;(respond {:status 200 :body "Thanks!"})
            new-resource))

        spin.resource/DELETE
        (delete [resource-provider server resource response request respond raise]
          (crux/submit-tx
           crux
           [[:crux.tx/delete (:crux.db/id resource)]])
          (respond (assoc response :status 204))))

      ;; Server capabilities
      (reify
        spin.server/ServerOptions
        (server-header [_] "Flux (JUXT), Vert.x")
        (server-options [_] nil)

        spin.server/RequestBody
        (request-body-as-bytes [_ request cb]
          (flux/handle-body
           request
           (fn [buffer]
             (cb (.getBytes buffer)))))))

     (wrap-crux-db-snapshot crux))))
