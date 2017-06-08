(ns edge.starwars-app
  (:require
   [yada.yada :as yada]
   [selmer.parser :as selmer]))

(defn starwars-app-routes [db {:keys [port]}]
  ["/starwars-app"
   (yada/resource
    {:id :edge.resources/starwars-app
     :methods
     {:get
      {:produces "text/html"
       :response
       (fn [ctx] (selmer/render-file
                  "sw-app.html"
                  {:title "Star Wars app"
                   :ctx ctx
                   }))}}})])
