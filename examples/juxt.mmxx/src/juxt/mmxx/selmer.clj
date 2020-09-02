;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.selmer
  (:refer-clojure :exclude [compile])
  (:require
   [selmer.parser :as selmer]
   [selmer.util :refer [*custom-resource-path*]]
   [crux.api :as crux]
   [juxt.mmxx.compiler :refer [PayloadCompiler]]))

(def templates-source-uri (java.net.URI. "http://localhost:2020/_templates/"))

(defrecord SelmerTemplator []
  PayloadCompiler
  (payload [this]
    (let [db (:crux/db this)
          eid (:crux.cms.selmer/template this)
          template-ent (crux/entity db eid)]
      (when-not template-ent
        (throw (ex-info "Failed to find template entity" {:eid eid})))

      (binding [*custom-resource-path* (.toURL templates-source-uri)]
        (selmer/render-file
         (java.net.URL. (str templates-source-uri "index.html"))
         this))))

  (last-modified-date [this]
    ;; Should be the most recent of this and any dependencies (such as :crux.cms.selmer/template)
    (let [db (:crux/db this)
          eid (:crux.cms.selmer/template this)
          e-tx (crux/entity-tx db eid)]
      (:crux.db/valid-time e-tx))))
