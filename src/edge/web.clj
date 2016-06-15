;; Copyright © 2016, JUXT LTD.

(ns edge.web
  "URI Routes to web content"
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [selmer.parser :as selmer]
   [yada.yada :as yada]
   ))

(defn templated-page-resource [template view-model-fn]
  (yada/resource
   {:methods
    {:get
     {:produces #{"text/html"}
      :response (fn [ctx]
                  (let [view-model (view-model-fn ctx)]
                    (selmer/render-file template view-model)))}}}))

(defn create-uri-for-model-entry [])

(defn content-routes [deps]
  ["/"
   [
    ["index.html"
     (->
      (templated-page-resource "index.html" (fn [ctx] {:foo "bar" :ctx ctx}))
      (assoc :id ::index))]

    ["" (yada/redirect ::index)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.

    ["test" (yada/handler "this is a test")]


    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id ::static))]

    ]])
