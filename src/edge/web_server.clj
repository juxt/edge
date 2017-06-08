;; Copyright © 2016, JUXT LTD.

(ns edge.web-server
  (:require
   [aleph.http :as http]
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clj-http.client :as http2]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [edge.sources :refer [source-routes]]
   [hiccup.core :refer [html]]
   [manifold.deferred :as d]
   [manifold.stream :as ms]
   [edge.examples :refer [authentication-example-routes]]
   [edge.phonebook :refer [phonebook-routes]]
   [edge.phonebook-app :refer [phonebook-app-routes]]
   [edge.starwars-app :refer [starwars-app-routes]]
   [edge.hello :refer [hello-routes other-hello-routes]]
   [selmer.parser :as selmer]
   [schema.core :as s]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]))

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

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :edge.resources/static))]]])

(defn my-hello-routes []
  [""
   [

    ["/hello3"
     (yada/resource
      {:methods
       {:get
        {:produces [{:media-type "text/html"
                     :language #{"en" "fr" "pt"}}

                    ]
         :response (fn [ctx]
                     (hiccup.core/html
                      [:p "You really want to go "
                       [:a
                        {:href (yada/href-for ctx :hello2 {:route-params {:accid "23876"}})}
                        "here"]]))}}})]

    [["/" :accid "/hello2"]
     (yada/resource
      {:id :hello2
       :methods
       {:get
        {:parameters
         {:query {(s/optional-key :greetee) String}
          :path {:accid String}}
         :produces [{:media-type "text/html"
                     :language #{"en" "fr" "pt"}}
                    {:media-type "application/json"
                     :language #{"en" "fr" "pt"}}
                    ]
         :response (fn [ctx]
                     (case (yada/content-type ctx)
                       "text/html"
                       (hiccup.core/html
                        [:body
                         [:p {:style "color: purple"} "Hello " (-> ctx :parameters :query :greetee)]])

                       "application/json"
                       (assoc (-> ctx :parameters)
                              :lang (yada/language ctx))))}}})]]])

(defn starwars-routes []
  ["/starwars"
   [
    ["/messages" (yada/resource
                  {:methods
                   {:get
                    {:produces "text/event-stream"
                     :parameters {:query {:period Long}}
                     :response
                     (fn [ctx]
                       (let [n (atom 0)]
                         (ms/periodically (-> ctx :parameters :query :period) (fn [] (swap! n inc)))))}}})]
    ["/people"
     (yada/resource
      {:methods
       {:get
        {:produces "application/edn"
         :response
         (fn [ctx]
           ;; Synchronous
           (:body (http2/get "https://swapi.co/api/people" {:as :json}))

           ;; Asynchronous
           #_(manifold.deferred/chain
              (aleph.http/get "https://swapi.co/api/people" #_{:headers {"accept" "application/json"}})
              :body

              ))}}})]]])

(defn routes
  "Create the URI route structure for our application."
  [db config]
  [""
   [
    ;; Exercise: Create "Hello World" here!
    ["/hello" (fn [req] {:status 200 :body "Hello World!"})]

    ;; s is schema.core

    (my-hello-routes)
    (starwars-routes)

    ["/api" (yada/swaggered
             (starwars-routes)
             {:info {:title "This is my API"
                     :version "1.0"
                     :description "An API on the classic example"}
              :basePath "/api"})]

    (phonebook-routes db config)
    (phonebook-app-routes db config)
    (starwars-app-routes db config)

    (authentication-example-routes)

    ;; Swagger UI
    #_["/swagger" (-> (new-webjar-resource "/swagger-ui" {:index-files ["index.html"]})
                      ;; Tag it so we can create an href to the Swagger UI
                      (tag :edge.resources/swagger))]

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

(defrecord WebServer [host
                      port
                      db
                      listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model (vhosts-model [{:scheme :http :host host} (routes db {:port port})])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server [m]
  (using
   (map->WebServer m)
   [:db]))
