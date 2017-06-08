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
   [clojure.core.async :as a]
   [selmer.parser :as selmer]
   [schema.core :as s]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   [clojure.string :as str]))

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

(defmethod yada.security/verify :r2d2
  [ctx scheme]
  ;; TODO: Check that R2D2 has identified himself in the request headers!!
  (when (= (get-in ctx [:request :headers "identification"]) "R2D2")
    {:roles #{:starwars/droid}}))

(def vader-censor (filter (comp not #(re-seq #"vader" %) clojure.string/lower-case)))

(defn starwars-routes [sending-channel mult]
  ["/starwars"
   [
    ["/messages" (yada/resource
                  {:methods
                   {:get
                    {:produces "text/event-stream"
                     :response (fn [ctx] (a/tap mult (a/chan 1 vader-censor)))}}})]

    ["/send-message" (yada/resource
                      {:access-control
                       {:scheme :r2d2
                        :authorization {:methods {:post :starwars/droid}}
                        }
                       :methods
                       {:post
                        {:parameters {:form {:message String}}
                         :consumes "application/x-www-form-urlencoded"
                         :response (fn [ctx]
                                     (a/put! sending-channel (-> ctx :parameters :form :message))
                                     (format "Thanks for the message, %s! "
                                             (-> ctx :request :headers (get "identification"))
                                             ))}}})]

    ["/people"
     (yada/resource
      {:methods
       {:get
        {:produces "application/edn"
         :response
         (fn [ctx]
           ;; Synchronous
           (:body (http2/get "https://swapi.co/api/people" {:as :json})))}}})]]])

(defn routes
  "Create the URI route structure for our application."
  [db config]
  [""
   [
    ;; Exercise: Create "Hello World" here!
    ["/hello" (fn [req] {:status 200 :body "Hello World!"})]

    (my-hello-routes)
    (starwars-routes (:chan config) (:mult config))

    #_["/api" (yada/swaggered
             (starwars-routes (:chan config))
             {:info {:title "This is my API"
                     :version "1.0"
                     :description "An API on the classic example"}
              :basePath "/api"})]

    (phonebook-routes db config)
    (phonebook-app-routes db config)
    (starwars-app-routes db config)

    (authentication-example-routes)

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
    (let [c (a/chan 2)
          m (a/mult c)]
      (if listener
        component                         ; idempotence
        (let [vhosts-model (vhosts-model [{:scheme :http :host host} (routes db {:port port :chan c :mult m})])
              listener (yada/listener vhosts-model {:port port})]
          (infof "Started web-server on port %s" (:port listener))
          (assoc component :listener listener :chan c :mult m)))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server [m]
  (using
   (map->WebServer m)
   [:db]))
