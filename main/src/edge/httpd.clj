;; Copyright © 2016, JUXT LTD.

(ns edge.httpd
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   edge.yada.lacinia
   [edge.examples :refer [authentication-example-routes]]
   [edge.hello :refer [hello-routes other-hello-routes]]
   [hiccup.core :refer [html]]
   [integrant.core :as ig]
   [ring.util.mime-type :refer [ext-mime-type]]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]))

;; Candidate for promotion to yada to add to yada's version
(defn new-resources-resource
  [root-path]
  (letfn [(file-of-resource [res]
            (case (.getProtocol res)
              "jar" (-> (.. res openConnection getJarFileURL getFile))
              "file" (.. res getFile)))]
    (resource
      {:path-info? true
       :properties
       (fn [ctx]
         (if-let [res (io/resource (str root-path (-> ctx :request :path-info)))]
           {:last-modified (some-> res file-of-resource io/file (.lastModified))}
           {}))
       :methods
       {:get
        {:produces
         (fn [ctx]
           (let [path (-> ctx :request :path-info)]
             (let [[mime-type typ _]
                   (re-matches
                     #"(.*)/(.*)"
                     (or (ext-mime-type path) "text/plain"))]
               (merge
                 {:media-type mime-type}
                 (when (= typ "text") {:charset "UTF-8"})))))
         :response
         (fn [ctx]
           (when-let [res (io/resource (str root-path (-> ctx :request :path-info)))]
             (.openStream res)))}}})))

(defn content-routes []
  ["/"
   [
    ["public/" (assoc (new-resources-resource "public/") :id :static)]]])

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [
    ;; Document routes
    ["/" (yada/redirect (:edge.httpd/index config))]

    ["" (:edge.httpd/routes config)]

    ;; Hello World!
    (hello-routes)
    (other-hello-routes)

    (authentication-example-routes)

    ["/api" (-> (hello-routes)
                ;; Wrap this route structure in a Swagger
                ;; wrapper. This introspects the data model and
                ;; provides a swagger.json file, used by Swagger UI
                ;; and other tools.
                (yada/swaggered
                  {:info {:title "Hello World!"
                          :version "1.0"
                          :description "An API on the classic example"}
                   :basePath "/api"})
                ;; Tag it so we can create an href to this API
                (tag :edge.resources/api))]

    ;; Swagger UI
    ;; TODO: extract to module?
    ["/swagger" (-> (new-webjar-resource "/swagger-ui" {:index-files ["index.html"]})
                    ;; Tag it so we can create an href to the Swagger UI
                    (tag ::swagger))]

    ;;(graphql/routes config)

    ;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we go there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])

;; TODO: Rename this package and component to listener
(defmethod ig/init-key :edge/httpd
  [_ {:edge.httpd/keys [host port] :as config}]
  (let [vhosts-model (vhosts-model [{:scheme :http :host host} (routes config)])
        listener (yada/listener vhosts-model {:port port})]
    (log/infof "Started http server on port %s" (:port listener))
    {:listener listener
     ;; host is used for announcement in dev
     :host host}))

(defmethod ig/halt-key! :edge/httpd [_ {:keys [listener]}]
  (when-let [close (:close listener)]
    (close)))
