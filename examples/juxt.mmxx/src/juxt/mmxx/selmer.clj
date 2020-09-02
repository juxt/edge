;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.selmer
  (:refer-clojure :exclude [compile])
  (:require
   [selmer.parser :as selmer]
   [selmer.util :refer [*custom-resource-path*]]
   [crux.api :as crux])  )

(def templates-source-uri (java.net.URI. "http://localhost:2020/_templates/"))

(defn compile [db representation-metadata]

  ;; 1. Load template entity
  (let [eid (:crux.cms.selmer/template representation-metadata)
        template-ent (crux/entity db eid)]
    (when-not template-ent
      (throw (ex-info "Failed to find template entity" {:eid eid})))

    (binding [*custom-resource-path* (.toURL templates-source-uri)]
      (selmer/render-file
       (java.net.URL. (str templates-source-uri "index.html"))
       representation-metadata))))
