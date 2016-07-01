;; Copyright © 2016, JUXT LTD.

;; Our phonebook but as a single page application (SPA)

(ns edge.phonebook-app
  (:require
   [yada.yada :as yada]
   [selmer.parser :as selmer]))

(defn phonebook-app-routes [db {:keys [port]}]
  ["/phonebook-app"
   (yada/resource
    {:id :edge.resources/phonebook-app
     :methods
     {:get
      {:produces "text/html"
       :response
       (fn [ctx] (selmer/render-file
                  "phonebook-app.html"
                  {:title "Edge phonebook app"
                   :ctx ctx
                   }))}}})])
