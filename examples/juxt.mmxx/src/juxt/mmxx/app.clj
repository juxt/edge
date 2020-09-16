;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.app
  (:require
   [clojure.java.io :as io]
   [crux.api :as crux]
   [juxt.mmxx.util :as util]
   [juxt.spin.alpha.handler :as spin.handler]
   [juxt.spin.alpha.resource :as spin.resource]
   [juxt.spin.alpha.server :as spin.server]
   [juxt.pick.alpha.core :refer [pick]]
   [juxt.pick.alpha.apache :refer [using-apache-algo]]
   [juxt.reap.alpha.ring :refer [decode-accept-headers]]
   [juxt.reap.alpha.decoders :refer [content-type]]
   [juxt.mmxx.compiler :as compiler]
   [juxt.flux.helpers :as a]
   [selmer.parser :as selmer]
   [juxt.flow.protocols :as flow]
   [juxt.flux.flowable :as f]
   [clojure.string :as str])
  (:import
   (io.reactivex Flowable)))

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
                         :juxt.http/last-modified (compiler/last-modified-date compiler)
                         ;; TODO: Add :juxt.http/entity-tag
                         ))))

            ;; If we can't find a resource we usually return nil. However, we may
            ;; decide to still return a resource if no resource is found in the
            ;; database, because we might want to represent the case of a resource
            ;; that does not have a representation in the database, but where a PUT
            ;; might create one.

            ;; In this example, we'll to accept anything that is a file in the
            ;; /flux area.
            (if (re-matches #"/flux/[A-Za-z0-9-]+(?:\.[A-Za-z0-9]+)?" (.getPath (new java.net.URI uri)))
              {:juxt.http/uri uri}

              ;; 404
              nil)))

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

            ;; Can we create a form?
            true
            (let [file-type-map {"txt" ["text/plain"]
                                 "html" ["text/html"]
                                 "jpg" ["image/jpeg"]
                                 "jpeg" ["image/jpeg"]
                                 "mp4" ["video/mp4"]}
                  suffix (second (re-find #"(?:\.([^.]+))?$" (:juxt.http/uri resource)))
                  file-types (file-type-map suffix)
                  accept (when file-types (str/join "," file-types))]
              (respond
               (-> response
                   (assoc :status 404)
                   (update :headers conj ["content-type" "text/html;charset=utf-8"])
                   (assoc :body
                          (selmer/render
                           (slurp (io/resource "juxt/mmxx/empty-get.html"))
                           {:resource (pr-str resource)
                            :accept accept
                            :content-type (first file-types)})))))

            :else (respond {:status 404})))

        spin.resource/POST
        (post [resource-provider server-provider resource response {:keys [crux/db] :as request} respond raise]


          (let [{:juxt.http/keys [type subtype]} (content-type (get-in request [:headers "content-type"]))]
            (if (and (= type "multipart") (= subtype "form-data"))
              (spin.server/request-body-as-multipart-bytes
               server-provider resource response request respond raise)
              (throw (ex-info "TODO" {})))


            )

          ;; Auth check ! Are they a super-user - account owner?

          )

        spin.resource/PUT
        (put [resource-provider server-provider resource response {:keys [crux/db] :as request} respond raise]

          ;; Auth check ! Are they a super-user - account owner?
          (let [{:juxt.http/keys [type subtype]} (content-type (get-in request [:headers "content-type"]))]
            (if (and (= type "multipart") (= subtype "form-data"))
              #_(spin.server/request-body-as-multipart-bytes
                 server-provider resource response request respond raise)

              ;; TODO: We should attempt to write the multipart logic as a
              ;; single-threaded test.

              (->>
               (spin.server/receive-multipart-body
                server-provider
                response request respond raise)

               ;; Map over each part
               (f/map
                (fn [part]
                  (->>
                   (:byte-source part)
                   (f/ignore-elements)
                   ;; TODO: Instead of ignoreElements we should hand this
                   ;; publisher off to a backend 'content store'.  This
                   ;; should return the blake2 content hash of the buffers
                   ;; as a 'single'.
                   (f/do-on-complete
                    #(println "Part upload of" (:name part) "complete!!"))
                   (f/subscribe))
                  (Flowable/just :ok)))

               (f/do-on-complete
                (fn [] (respond response)))

               (f/subscribe))

              #_(let [new-resource
                      (into
                       resource

                       (case (get-in request [:headers "content-type"])
                         "vnd.crux.entity+edn"
                         (conj
                          (edn/read-string (new String (:juxt.http/payload representation-in-request) "UTF-8"))
                          ;; Never allow the PUTer to override the value of
                          ;; :juxt.http/uri - this should be missing from the incoming
                          ;; payload but should be added prior to puting in the
                          ;; database.
                          [:juxt.http/uri (:juxt.http/uri resource)])

                         ;; Else
                         (concat
                          (dissoc representation-in-request :juxt.http/payload)
                          {:juxt.http/base64-encoded-payload (.encodeToString encoder (:juxt.http/payload representation-in-request))
                           :juxt.http/methods #{:get :put :options}
                           :juxt.http/uri (:juxt.http/uri resource)})))]

                  (crux/await-tx crux (crux/submit-tx crux [[:crux.tx/put new-resource]]))

                  (let [e-hist (crux/entity-tx (crux/db crux) (:crux.db/id new-resource))]
                    (cond-> new-resource
                      (:crux.db/valid-time e-hist) (assoc :juxt.http/last-modified (:crux.db/valid-time e-hist))
                      (:crux.db/content-hash e-hist) (assoc :juxt.http/entity-tag (str "\"" (:crux.db/content-hash e-hist) "\""))
                      ;; We could respond here, or we return a new resource for the Spin to respond
                      ;;(respond {:status 200 :body "Thanks!"})
                      )))

              ;; We should open a temporary file and stream the video into it
              ;; In this code we cannot assume vert.x/flux (although we can assume flow)
              (-> (util/stream-request-body-to-file
                   server-provider
                   request
                   (fn [] (respond response))
                   raise)
                  (util/wrap-temp-file "flux" (second (re-find #"(?:(\.[^.]+))?$" (:juxt.http/uri resource))) request respond raise))

              #_(spin.server/subscribe-to-request-body
                 server-provider request

                 #_(reify flow/Subscriber
                     (on-subscribe [_ subscription]
                       (println "app: on-subscribe: " "subscription is" (clojure.core/type subscription))
                       (.request subscription 10000)
                       #_(.onSubscribe s (reify org.reactivestreams.Subscription
                                           (cancel [_] (flow/cancel subscription))
                                           (request [_ n] (flow/request subscription n)))))
                     (on-error [_ t]
                       (println "on-error, t is " t)
                       )
                     (on-next [_ item]
                       (println "on-next, item type is" (clojure.core/type item))
                       )
                     (on-complete [_]
                       (println "on-complete")))))))


        spin.resource/DELETE
        (delete [resource-provider server resource response request respond raise]
          (crux/submit-tx
           crux
           [[:crux.tx/delete (:crux.db/id resource)]])
          (respond (assoc response :status 204))))

      ;; Server capabilities - these should be moved into flux (arguably) - is
      ;; it OK for flux to depend upon spin? if not, think of another project -
      ;; e.g. spin.flux, spin.jetty, spin.aleph
      (reify
        spin.server/ServerOptions
        (server-header [_] "Flux (JUXT), Vert.x")
        (server-options [_] nil)

        #_spin.server/RequestBody
        #_(request-body-as-bytes [_ request cb]
            (flux/handle-body
             request
             (fn [buffer]
               (cb (.getBytes buffer)))))

        #_(request-body-as-multipart-bytes [_ resource response request respond raise]
            (let [res-builder (atom {})]
              (.setExpectMultipart (:juxt.flux/request request) true)

              (.uploadHandler
               (:juxt.flux/request request)
               (a/h
                (fn [upload]
                  (println "Upload began of" (.name upload))
                  (let [b (Buffer/buffer)]
                    (.handler upload (a/h (fn [buf] (.appendBuffer b buf))))
                    (.endHandler
                     upload
                     (a/h
                      (fn [_]
                        (let [k (keyword (.name upload))]
                          (case k
                            :juxt.http/payload
                            (swap! res-builder assoc
                                   :juxt.http/base64-encoded-payload
                                   (.encodeToString encoder (.getBytes b)))
                            (swap! res-builder assoc
                                   k
                                   (.getBytes b)))))))))))

              (.endHandler
               (:juxt.flux/request request)
               (a/h
                (fn [_]
                  (let [attrs (.formAttributes (:juxt.flux/request request))]
                    (println "END of multipart, attrs are" (pr-str attrs))
                    (apply
                     swap! res-builder conj
                     (for [[k v] attrs]
                       [(keyword k) (edn/read-string v)]))

                    (respond response)
                    #_(let [new-resource (assoc @res-builder
                                                :juxt.http/uri (:juxt.http/uri resource)
                                                ;;:crux.db/id (UUID/randomUUID)
                                                )]
                        (println "res is" (pr-str new-resource))

                        (crux/await-tx crux (crux/submit-tx crux [[:crux.tx/put new-resource]]))

                        (respond
                         (let [e-hist (crux/entity-tx (crux/db crux) (:crux.db/id new-resource))]
                           (cond-> response
                             (:crux.db/valid-time e-hist)
                             (update :headers (fnil conj {})
                                     ["last-modified" (spin.util/format-http-date (:crux.db/valid-time e-hist))])

                             (:crux.db/content-hash e-hist)
                             (update :headers (fnil conj {})
                                     ["etag" (str "\"" (:crux.db/content-hash e-hist) "\"")])

                             )))))))))

            #_(throw (ex-info "TODO" {}))

            #_(flux/handle-body
               request
               (fn [buffer]
                 (cb (.getBytes buffer)))))


        spin.server/ReactiveStreamable
        (subscribe-to-request-body [_ request subscriber]
          (flow/subscribe
           (.toFlowable (:juxt.flux/request request))
           subscriber))

        (receive-multipart-body [_ #_receiver response request respond raise]
          (.setExpectMultipart (:juxt.flux/request request) true)

          (Flowable/create
           (reify io.reactivex.FlowableOnSubscribe
             (subscribe [_ emitter]
               (.uploadHandler
                (:juxt.flux/request request)
                (a/h
                 (fn [upload]
                   (.onNext
                    emitter
                    {:name (.name upload)
                     :content-type (.contentType upload)
                     :byte-source (.toFlowable upload)
                     :juxt.flux/upload upload}))))
               (.endHandler
                (:juxt.flux/request request)
                (a/h
                 (fn [_]
                   (.onComplete emitter))))))
           io.reactivex.BackpressureStrategy/BUFFER))))

     (wrap-crux-db-snapshot crux))))
