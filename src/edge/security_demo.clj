;; Copyright © 2016, JUXT LTD.

(ns edge.security-demo
  (:require
   [yada.yada :as yada]
   [hiccup.core :refer [html]]))

(defn security-demo-routes []
  ["/basic"
   (yada/resource
    {:id :edge.resources/basic
     :access-control
     {:scheme "Basic"

      :verify (fn [[user password]]
                (when (= [user password] ["alice" "clojurerocks!"])
                  {:user "alice"
                   :roles #{:user}}))

      :authorization {:methods {:get :user}}}

     :methods
     {:get {:produces "text/html"
            :response (fn [ctx]
                        (html [:body
                               [:h1 "Congratulations!"]
                               [:p "You've got through security!"]]))}}})])
