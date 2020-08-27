;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.crux
  (:require
   [integrant.core :as ig]
   [crux.api :as crux]
   [clojure.java.io :as io])
  )

(defmethod ig/init-key ::node [_ _]
  (crux/start-node {:crux.node/topology 'crux.standalone/topology}))

(defmethod ig/halt-key! ::node [_ node]
  (.close node))
