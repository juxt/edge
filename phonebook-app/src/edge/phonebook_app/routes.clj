;; Copyright © 2016, JUXT LTD.

;; Our phonebook but as a single page application (SPA)

(ns edge.phonebook-app.routes
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [selmer.parser :as selmer]
   [yada.yada :as yada]))

(defn- routes
  [{:edge.phonebook/keys [db]}]
  [["" (yada/resource
          {:id ::phonebook-app
           :methods
           {:get
            {:produces "text/html"
             :response
             (fn [ctx]
               (selmer/render-file
                 "phonebook-app.html"
                 {:ctx ctx}
                 {:custom-resource-path (io/resource "phonebook-app/templates/")}))}}})]])

(defmethod ig/init-key :edge.phonebook-app/routes [_ config]
  (routes config))
