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
   [juxt.mmxx.compiler :as compiler]
   [juxt.flux.api :as flux]
   [juxt.flux.helpers :as a])
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
            (let [e-hist (crux/entity-tx db (:crux.db/id e))
                  e (cond-> e
                      (:juxt.http/content-type e) (update :juxt.http/content-type memoized-content-type))]
              (cond
                ;; Base64 encoded content indicates a static representation
                (:juxt.http/base64-encoded-payload e)
                (assoc e
                       :juxt.http/payload (byte-array [])
                       :juxt.http/last-modified (:crux.db/valid-time e-hist)
                       :juxt.http/entity-tag (str "\"" (:crux.db/content-hash e-hist) "\""))

                ;; Base64 encoded content indicates a dynamic (generated) representation
                (:crux.cms/compiler e)
                (let [compiler-eid (:crux.cms/compiler e)
                      compiler-doc (crux/entity db compiler-eid)
                      _ (assert compiler-doc)
                      s (:crux.cms/compiler-constructor compiler-doc)
                      _ (assert s (pr-str compiler-doc))
                      _ (require (symbol (namespace s)))
                      constructor (resolve s)
                      compiler (constructor (assoc e :crux/db db))]
                  (assoc e
                         :crux.cms/compiler-impl compiler
                         :juxt.http/last-modified (compiler/last-modified-date compiler))

                  ;; TODO: Add :juxt.http/last-modified and :juxt.http/entity-tag to e via compiler
                  #_(assoc :juxt.http/last-modified (:crux.db/valid-time e-hist)
                           :juxt.http/entity-tag (str "\"" (:crux.db/content-hash e-hist) "\"")))

                ))

            ;; If we can't find a resource we usually return nil. However, we may
            ;; decide to still return a resource if no resource is found in the
            ;; database, because we might want to represent the case of a resource
            ;; that does not have a representation in the database, but where a PUT
            ;; might create one.

            ;; In this example, we'll to accept anything that is a file in the
            ;; /flux area.
            (when (re-matches #"/flux/[A-Za-z-]+(?:\.[A-Za-z]+)?" (.getPath uri))
              {:crux.db/id (UUID/randomUUID)
               :juxt.http/uri uri})))

        spin.resource/Representation
        (representation [resource-provider resource {:keys [crux/db] :as request}]
          (cond
            (:juxt.http/variants resource)
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

            ;; Maybe have a new protocol which allows a representation to be established/generated
            ;; Or maybe the contract of _this_ protocol is to return a fully fledged representation
            ;; Generate?

            ;; No content negotiation, return the resource
            :else resource))

        spin.resource/GET
        (get-or-head [resource-provider server-provider resource response
                      {:keys [crux/db] :as request} respond raise]
          (cond
            ;; We have a representation
            (:juxt.http/base64-encoded-payload resource)
            (let [payload-bytes (.decode decoder (:juxt.http/base64-encoded-payload resource))]
              (respond
               (cond-> response
                 true (update :headers conj ["content-length" (str (count payload-bytes))])
                 (= (:request-method request) :get) (conj {:body payload-bytes}))))

            ;; Redirect?
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

            ;; Compiler?
            (:crux.cms/compiler-impl resource)
            (let [compiler-impl (:crux.cms/compiler-impl resource)]
              (a/execute-blocking-code
               (:juxt.flux/vertx request)
               (fn [] (compiler/payload compiler-impl))
               {:on-success
                (fn [payload]
                  (respond
                   (assoc response :body payload)))
                :on-failure
                (fn [t]
                  (raise
                   (ex-info "Failed to compile" {:resource resource} t)))}))

            :else
            (respond {:status 404})))

        spin.resource/PUT
        (put [resource-provider representation-in-request resource response {:keys [crux/db] :as request} respond raise]

          ;; Auth check ! Are they a super-user - account owner

          (let [new-resource
                (into
                 {:crux.db/id (:crux.db/id resource)
                  :juxt.http/uri (:juxt.http/uri resource)}
                 (concat
                  (dissoc representation-in-request :juxt.http/payload)
                  {:juxt.http/base64-encoded-payload (.encodeToString encoder (:juxt.http/payload representation-in-request))
                   :juxt.http/methods #{:get :put :options}}))]

            (crux/await-tx crux (crux/submit-tx crux [[:crux.tx/put new-resource]]))

            (let [e-hist (crux/entity-tx (crux/db crux) (:crux.db/id new-resource))]
              (cond-> new-resource
                (:crux.db/valid-time e-hist) (assoc :juxt.http/last-modified (:crux.db/valid-time e-hist))
                (:crux.db/content-hash e-hist) (assoc :juxt.http/entity-tag (str "\"" (:crux.db/content-hash e-hist) "\""))
                ;; We could respond here, or we return a new resource for the Spin to respond
                ;;(respond {:status 200 :body "Thanks!"})
                ))))

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

;;(pr-str debug)
