;; Copyright © 2016, JUXT LTD.

(ns edge.sources
  (:require
   [clojure.java.io :as io]
   [yada.yada :as yada]))

(defn source-routes
  "Return a route that serves up source code as-is in raw text/plain
  format."
  []
  ["/sources/"
   (yada/resource
    {
     ;; We enable path-info which means that the route matches any
     ;; path that matches /sources/*.
     :path-info? true

     :properties
     (fn [ctx]
       ;; Use the path-info as the relative path of a source file
       (if-let [f (yada/safe-relative-file
                   (System/getProperty "user.dir")
                   (-> ctx :request :path-info))]
         ;; Valid file path, possibly we exist too. We add the file as a
         ;; property of the resource.
         {:exists? (.exists f)
          ::file f}
         ;; Invalid file, so we don't exist
         {:exists? false}))

     :methods
     {:get {
            ;; Serve as 'raw' (text/plain)
            :produces {:media-type "text/plain"
                       :charset "UTF-8"}
            ;; Return the file we found as the response.
            :response #(-> % :properties ::file)}}})])
