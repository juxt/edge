;; Copyright © 2016, JUXT LTD.

(ns edge.phonebook
  (:require
   [bidi.bidi :as bidi]
   [edge.phonebook.db :as db]
   [clojure.tools.logging :refer :all]
   [hiccup.core :refer [html]]
   [selmer.parser :as selmer]
   [schema.core :as s]
   [yada.swagger :as swagger]
   [yada.yada :as yada]))

(defn- entry-map->vector [ctx m]
  (sort-by
   :id
   (reduce-kv
    (fn [acc k v]
      (conj acc
            (assoc v
                   :id k
                   :href (yada/href-for
                          ctx
                          :edge.resources/phonebook-entry
                          {:route-params {:id k}}))))
    [] m)))

(defn new-index-resource [db]
  (yada/resource
   {:id :edge.resources/phonebook-index
    :description "Phonebook entries"
    :produces [{:media-type
                #{"text/html" "application/edn;q=0.9" "application/json;q=0.8" "application/transit+json;q=0.7"}
                :charset "UTF-8"}]
    :methods
    {:get {:parameters {:query {(s/optional-key :q) String}}
           :swagger/tags ["default" "getters"]
           :response (fn [ctx]
                       (let [q (get-in ctx [:parameters :query :q])
                             entries (if q
                                       (db/search-entries db q)
                                       (db/get-entries db))]
                         (case (yada/content-type ctx)
                           "text/html" (selmer/render-file
                                        "phonebook.html"
                                        {:title "Edge phonebook"
                                         :ctx ctx
                                         :entries (entry-map->vector ctx entries)
                                         :q q})
                           entries)))}

     :post {:parameters {:form {:surname String :firstname String :phone String}}
            :consumes #{"application/x-www-form-urlencoded"}
            :response (fn [ctx]
                        (let [id (db/add-entry db (get-in ctx [:parameters :form]))]
                          (java.net.URI. (:uri (yada/uri-info ctx :edge.resources/phonebook-entry {:route-params {:id id}})))))}}}))

(defn new-entry-resource [db]
  (yada/resource
   {:id :edge.resources/phonebook-entry
    :description "Phonebook entry"
    :parameters {:path {:id Long}}
    :produces [{:media-type #{"text/html"
                              "application/edn;q=0.9"
                              "application/json;q=0.8"
                              "application/transit+json;q=0.7"}
                :charset "UTF-8"}]
    :methods
    {:get
     {:swagger/tags ["default" "getters"]
      :response
      (fn [ctx]
        (let [id (get-in ctx [:parameters :path :id])
              {:keys [firstname surname phone] :as entry} (db/get-entry db id)]
          (when entry
            (case (yada/content-type ctx)
              "text/html" (selmer/render-file
                           "phonebook-entry.html"
                           {:title "Edge phonebook"
                            :entry entry
                            :ctx ctx
                            :id id})

              entry))))}

     :put
     {:parameters
      {:form {:surname String
              :firstname String
              :phone String}}

      :consumes
      [{:media-type #{"multipart/form-data"
                      "application/x-www-form-urlencoded"
                      "application/edn"}}]

      :response
      (fn [ctx]
        (let [entry (get-in ctx [:parameters :path :id])]
          (assert entry)
          (db/update-entry db entry
                           (or (get-in ctx [:parameters :form])
                               (:body ctx)))))}

     :delete
     {:produces "text/plain"
      :response
      (fn [ctx]
        (let [id (get-in ctx [:parameters :path :id])]
          (db/delete-entry db id)
          (let [msg (format "Entry %s has been removed" id)]
            (case (get-in ctx [:response :produces :media-type :name])
              "text/plain" (str msg "\n")
              "text/html" (html [:h2 msg])
              ;; We need to support JSON for the Swagger UI
              {:message msg}))))}}

    :responses {404 {:produces #{"text/html"}
                     :response (fn [ctx]
                                 (infof "parameters are '%s'" (:parameters ctx))
                                 (selmer/render-file
                                  "phonebook-404.html"
                                  {:title "No phonebook entry"
                                   :ctx ctx}))}}}))

(defn phonebook-routes [{:edge.phonebook/keys [db]
                         :edge.http/keys [port]}]
  (let [routes ["/phonebook"
                [
                 ;; Phonebook index
                 ["" (new-index-resource db)]
                 ;; Phonebook entry, with path parameter
                 [["/" :id] (new-entry-resource db)]]]]
    [""
     [
      routes

      ;; Swagger
      ["/phonebook-api/swagger.json"
       (bidi/tag
         (yada/handler
           (swagger/swagger-spec-resource
             (swagger/swagger-spec
               routes
               {:info {:title "Phonebook"
                       :version "1.0"
                       :description "A simple application that demonstrates the use of multiple HTTP methods"}
                :host (format "localhost:%d" port)
                :schemes ["http"]
                :tags [{:name "getters"
                        :description "All paths that support GET"}]
                :basePath ""})))
         :edge.resources/phonebook-swagger)]]]))
