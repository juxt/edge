;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.seeder
  (:require
   [integrant.core :as ig]
   [crux.api :as crux]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.util UUID)))

(def encoder (java.util.Base64/getEncoder))

(defmethod ig/init-key ::seeder [_ {:keys [crux]}]
  (crux/submit-tx
   crux
   (for [resource
         [ ;; Can this be PUT by using a special content-type of application/vnc.crux.entity+edn ?
          ;; Or via the http-server?
          ;; See https://www.iana.org/assignments/media-types/media-types.xhtml
          ;; Note, we can also add Content-Location as a request header, which solves this problem.

          {:crux.db/id :spin/root
           :juxt.http/uri "http://localhost:2020/"
           :juxt.http/redirect :spin/readme}

          ;; A resource which does proactive content negotiation to find the best representation
          {:crux.db/id :spin/readme
           :juxt.http/uri "http://localhost:2020/spin/README"
           :juxt.http/variants
           [:spin/readme-adoc :spin/readme-html]
           :juxt.http/methods #{:get :options}}

          {:crux.db/id :spin/readme-adoc
           :juxt.http/uri "http://localhost:2020/spin/README.adoc"

           ;; A resource corresponding to the adoc representation of the README
           :juxt.http/base64-encoded-payload
           (as-> "src/github.com/juxt/spin/README.adoc" %
             (io/file (System/getProperty "user.home") %)
             (slurp %)
             (.getBytes %"UTF-8")
             (.encodeToString encoder %))
           :juxt.http/content-type "text/plain;charset=utf-8"
           ;; slightly privilege the raw adoc over the html
           :juxt.http/quality-of-source 0.9

           ;; The :put indicates this is a 'source' document - maybe we can use this?
           :juxt.http/methods #{:get :put :options}}

          ;; A resource corresponding to the adoc representation of the README
          {:crux.db/id :spin/readme-html
           :juxt.http/uri "http://localhost:2020/spin/README.html"

           :juxt.http/quality-of-source 0.8
           :juxt.http/methods #{:get :options}

           ;; The last-modified-date and entity-tag are going to be computed
           ;; from dependencies, including :crux.cms/source and
           ;; :crux.cms/compiler (because changing the compiler might affect the
           ;; output!
           :crux.cms/source :spin/readme-adoc

           ;; The compiler is able to compute the dependency graph
           :crux.cms/compiler :ex/asciidoctor-builder}

          {:crux.db/id :ex/asciidoctor-builder}

          ;; Selmer
          {:crux.db/id :spin/index-template
           :juxt.http/uri "http://localhost:2020/_templates/index.html"
           :juxt.http/methods #{:get :put :options}
           :juxt.http/base64-encoded-payload
           (as-> "resources/templates/index.html" %
             (io/file (System/getProperty "user.dir") %)
             (slurp %)
             (.getBytes %"UTF-8")
             (.encodeToString encoder %))}

          {:crux.db/id :crux.cms.selmer/compiler
           :crux.cms/compiler-constructor 'juxt.mmxx.selmer/map->SelmerTemplator}

          ;; TODO: Think about the specification of 'collections', both for
          ;; WebDav and for sets of web resources under a given path, e.g. to
          ;; serve a file-system directory. This is conceptually similar to
          ;; relations. Maybe rename 'relations' to 'collections'?

          {:crux.db/id :spin/index-html
           :juxt.http/uri "http://localhost:2020/index.html"
           :juxt.http/methods #{:get :options}

           :juxt.http/content-type "text/html;charset=utf-8"

           :example/audience "World"

           ;; The compiler is able to compute the dependency graph
           :crux.cms/compiler :crux.cms.selmer/compiler
           :crux.cms.selmer/template :spin/index-template}]]

     [:crux.tx/put resource #inst "2020-02-29"])))
