;; Copyright © 2020, JUXT LTD.

(ns juxt.flux.ig
  (:require
   [integrant.core :as ig]
   [juxt.flux.api :as flux])
  (:import
   (io.vertx.reactivex.core Vertx)))

(defmethod ig/init-key ::vertx
  [_ _]
  (Vertx/vertx))

(defmethod ig/halt-key! ::vertx
  [_ vertx]
  (.close vertx))

(defmethod ig/init-key ::server
  [_ {:keys [handler] :as opts}]
  (flux/run-http-server (resolve handler) opts))

(defmethod ig/halt-key! ::server [_ server]
  (.close server))
