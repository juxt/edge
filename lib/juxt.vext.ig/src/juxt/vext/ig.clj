;; Copyright © 2020, JUXT LTD.

(ns juxt.vext.ig
  (:require
   [integrant.core :as ig]
   [juxt.vext.ring-server :as vextring])
  (:import
   (io.vertx.reactivex.core Vertx)))

(defmethod ig/init-key ::vertx
  [_ _]
  (Vertx/vertx))

(defmethod ig/halt-key! ::vertx
  [_ vertx]
  (.close vertx))


(defmethod ig/init-key ::server
  [_ {:keys [create-handler handler dynamic?] :as opts}]


  (vextring/run-http-server

   (cond
     handler
     (do
       (require (symbol (namespace handler)))
       (let [vr (resolve handler)]
         ;; If dynamic? is true then the handler remains as a var rather than a
         ;; function. This means that the var can change without requiring an
         ;; Integrant reset. If per-request performance is critical (in production),
         ;; dynamic? can be set to false.
         (if dynamic? vr @vr)))

     create-handler
     (do
       (require (symbol (namespace create-handler)))
       (let [vr (resolve create-handler)]
         ;; If dynamic? is true then the create-handler var function is called on
         ;; every request, and even a modified create-handler function only needs
         ;; to be re-evaled. This is great for development but comes with a
         ;; performance cost in production. So set dynamic? to to true for dev,
         ;; false for prod.
         (assert vr)
         (if dynamic?
           (fn [req respond raise]
             (let [handler (vr opts)]
               (assert handler)
               (handler req respond raise)))
           (vr opts))))

     :else (throw (ex-info "Illegal state, handler or create-handler must be given" {})))

   opts))

(defmethod ig/halt-key! ::server [_ server]
  (.close server))
