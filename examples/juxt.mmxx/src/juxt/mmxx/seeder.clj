;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.seeder
  (:require
   [integrant.core :as ig]
   [crux.api :as crux]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defmethod ig/init-key ::seeder [_ {:keys [crux]}]
  (crux/submit-tx
   crux
   (for [resource
         [
          {:crux.db/id (java.net.URI. "/documents/Crux-Form")
           :juxt.http/variants
           [(java.net.URI. "/documents/Crux-Form.adoc")
            (java.net.URI. "/documents/Crux-Form.html")]
           :juxt.http/methods #{:get :options}}

          {:crux.db/id (java.net.URI. "/documents/Crux-Form.adoc")
           ;; slightly privilege the raw adoc over the html
           :juxt.http/quality-of-source 0.9
           :juxt.http/content-type "text/plain;charset=utf-8"
           :juxt.http/methods #{:get :put :options}
           :content (str/join "\n" (take 20 (line-seq (io/reader (io/file (System/getProperty "user.home") "src/team/juxt.crux/collateral/projects/form-whitepaper/Crux-Form.adoc")))))}

          {:crux.db/id (java.net.URI. "/documents/Crux-Form.html")
           :juxt.http/quality-of-source 0.8
           :juxt.http/content-type "text/html;charset=utf-8"
           :juxt.http/methods #{:get :options}
           :content "<h2>TODO: This will be the HTML 'type' of the content</h2>\n"}]]

     [:crux.tx/put
      (cond-> resource
        ;; Set the juxt.http/uri to the same as the id
        true (assoc :juxt.http/uri (:crux.db/id resource))
        ;; Set the content length
        (:content resource) (assoc :juxt.http/content-length (count (:content resource)))
        )]

     )))
