;; Copyright © 2016, JUXT LTD.

(ns edge.server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [schema.core :as s]
   [yada.yada :refer [handler resource] :as yada]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [edge.api :refer [api-routes]]
   [edge.web :refer [content-routes]]))

(defn routes
  "Create the URI route structure for our application."
  []
  [""
   [["/api" (-> (api-routes {})
                ;; Wrap this route structure in a Swagger
                ;; wrapper. This introspects the data model and
                ;; provides a swagger.json file, used by Swagger UI
                ;; and other tools.
                (yada/swaggered
                 {:info {:title "Edge API"
                         :version "1.0"
                         :description "An example API"}
                  :basePath "/api"})
                ;; Tag it so we can create an href to this API
                (tag :edge.resources/api))]

    ;; Swagger
    ["/swagger" (-> (new-webjar-resource "/swagger-ui" {:index-files ["index.html"]})
                    ;; Tag it so we can create an href to the Swagger UI
                    (tag :edge.resources/swagger))]

    ;; Our content routes, and potentially other routes.
    (content-routes {})

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])



(s/defrecord WebServer [port :- s/Int
                        listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host (format "localhost:%d" port)}
              (routes)])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (dissoc component :listener)))

(defn new-web-server []
  (map->WebServer {}))
