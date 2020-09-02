;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.seeder
  (:require
   [integrant.core :as ig]
   [crux.api :as crux]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.util UUID)
   (java.net URI)))

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
           :juxt.http/uri (new URI "http://localhost:2020/")
           :juxt.http/redirect :spin/readme}

          ;; A resource which does proactive content negotiation to find the best representation
          {:crux.db/id :spin/readme
           :juxt.http/uri (new URI "http://localhost:2020/spin/README")
           :juxt.http/variants
           [:spin/readme-adoc :spin/readme-html]
           :juxt.http/methods #{:get :options}}

          {:crux.db/id :spin/readme-adoc
           :juxt.http/uri (new URI "http://localhost:2020/spin/README.adoc")

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
           :juxt.http/methods #{:get :put :options}

           :juxt.http/last-modified #inst "2020-08-01"}

          ;; A resource corresponding to the adoc representation of the README
          {:crux.db/id :spin/readme-html
           :juxt.http/uri (new URI "http://localhost:2020/spin/README.html")

           :juxt.http/base64-encoded-payload
           (.encodeToString
            encoder
            (.getBytes "<h2>TODO: This will be the generated HTML 'type' of the content</h2>\n"))
           :juxt.http/content-type "text/html;charset=utf-8"
           :juxt.http/quality-of-source 0.8

           :juxt.http/methods #{:get :options}

           :juxt.http/last-modified #inst "2020-08-08"}]]

     [:crux.tx/put
      (cond-> resource
        (:juxt.http/base64-encoded-payload resource)
        (assoc :juxt.http/entity-tag (str "\"" (hash (:juxt.http/base64-encoded-payload resource)) "\"")))])))
