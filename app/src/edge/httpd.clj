;; Copyright © 2016, JUXT LTD.

(ns edge.httpd
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   edge.graphql
   edge.yada.lacinia
   [edge.examples :refer [authentication-example-routes]]
   [edge.hello :refer [hello-routes other-hello-routes]]
   [edge.phonebook :refer [phonebook-routes]]
   [edge.phonebook-app :refer [phonebook-app-routes]]
   [edge.sources :refer [source-routes]]
   [hiccup.core :refer [html]]
   [integrant.core :as ig]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.resources.classpath-resource :refer [new-classpath-resource]]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   edge.yada.lacinia))

(defn content-routes []
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :edge.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "index.html" {:title "Edge Index"
                                                       :ctx ctx}))}}})]

    ["" (assoc (yada/redirect :edge.resources/index) :id :edge.resources/content)]
    ["public/" (assoc (new-classpath-resource "public") :id :edge.resources/static)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ]])

(defn routes
  "Create the URI route structure for our application."
  [{:edge.phonebook/keys [db]
    :edge/keys [graphql-schema event-bus]
    :as config}]
  [""
   [
    ;; Hello World!
    (hello-routes)
    (other-hello-routes)

    (phonebook-routes config)
    (phonebook-app-routes config)

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
    ["/swagger" (-> (new-webjar-resource "/swagger-ui" {:index-files ["index.html"]})
                    ;; Tag it so we can create an href to the Swagger UI
                    (tag :edge.resources/swagger))]

    ;; GraphQL
    ["/graphql"
     (yada/resource
       {:methods
        {:post
         {:consumes "application/json"
          :produces "application/json"
          :response (fn [ctx]
                      (edge.yada.lacinia/query db graphql-schema (-> ctx :body :query)))}}})]

    ["/graphql-stream"
     (yada/resource
       {:methods
        {:post
         {:consumes "application/json"
          :produces "text/event-stream"
          :response (fn [ctx]
                      (edge.yada.lacinia/subscription-stream db graphql-schema (-> ctx :body :query)))}}})]

    #_["/gtest"
     (fn [req]
       (let [s (ms/stream)]
         {:status 200 :body s}))
     ]

    ["/status" (yada/resource
                 {:methods
                  {:get
                   {:produces "text/html"
                    :response (fn [ctx]
                                (html
                                  [:body
                                   [:div
                                    [:h2 "System properties"]
                                    [:table
                                     (for [[k v] (sort (into {} (System/getProperties)))]
                                       [:tr
                                        [:td [:pre k]]
                                        [:td [:pre v]]]
                                       )]]
                                   [:div
                                    [:h2 "Environment variables"]
                                    [:table
                                     (for [[k v] (sort (into {} (System/getenv)))]
                                       [:tr
                                        [:td [:pre k]]
                                        [:td [:pre v]]]
                                       )]]
                                   ]))}}})]

    ;; The Edge source code is served for convenience
    (source-routes)

    ;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])

(defmethod ig/init-key :edge/httpd
  [_ {:edge.httpd/keys [host port] :as config}]
  (let [vhosts-model (vhosts-model [{:scheme :http :host host} (routes config)])
        listener (yada/listener vhosts-model {:port port})]
    (infof "Started http server on port %s" (:port listener))
    {:listener listener
     ;; host is used for announcement in dev
     :host host}))

(defmethod ig/halt-key! :edge/httpd [_ {:keys [listener]}]
  (when-let [close (:close listener)]
    (close)))
