;; Copyright © 2016, JUXT LTD.

(ns edge.web
  "URI Routes to web content"
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [selmer.parser :as selmer]
   [yada.yada :as yada]))

(defn content-routes [_]
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id ::index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "index.html" {:ctx ctx}))}}})]

    ["" (assooc (yada/redirect ::index) :id :edge.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example
    ["hello" (yada/handler "Hello World!\n")]

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id ::static))]]])
