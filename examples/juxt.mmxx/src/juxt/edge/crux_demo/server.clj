;; Copyright © 2020, JUXT LTD.

(ns juxt.edge.crux-demo.server
  (:require [juxt.flux.api :as flux]))

(defmethod ig/init-key ::vertx
  [_ _]
  (Vertx/vertx))

(defmethod ig/halt-key! ::vertx
  [_ vertx]
  (.close vertx))

(defmethod ig/init-key ::http-server
  [_ {:keys [router] :as opts}]
  (flux/run-http-server router opts))

(defmethod ig/halt-key! ::http-server [_ server]
  (.close server))
