(ns edge.examples
  (:require
   [buddy.sign.jwt :as jwt]
   [clojure.edn :as edn]
   [clojure.tools.logging :refer :all]
   [hiccup.core :refer [html]]
   [yada.yada :as yada]
   [yada.security :refer [verify]]))

(defn protected-response [ctx]
  (html
    [:body
      [:h1 "Congratulations!"]
      [:p "You're accessing a restricted resource!"]
     [:pre (pr-str (:authentication ctx))]]))

(defmethod verify :my-custom-authn
  [ctx scheme]
  (when-let [user (get-in ctx [:request :headers "x-whoami"])]
    {:user user
     :roles #{:user}}))

(def secret "ieXai7aiWafeSh6oowow")

(defmethod verify :my-jwt-scheme
  [ctx scheme]
  (infof "jwt is %s" (get-in ctx [:request :headers "x-whoami"]))
  (some->
   (get-in ctx [:request :headers "x-whoami"])
   (jwt/unsign secret)
   :claims
   edn/read-string))

(defn authentication-example-routes []
  ["/authn-examples"
   [
    ["/basic"
     (yada/resource
      {:id :edge.resources/basic-authn-example
       :methods
       {:get {:produces "text/html"
              :response (fn [ctx] (protected-response ctx))}}

       :access-control
       {:scheme "Basic"
        :verify (fn [[user password]]
                  (when (= user "alice")
                    {:user user
                     :roles #{:user}
                     }))
        :authorization {:methods {:get :user}}}})]

    ["/custom"
     (yada/resource
      {:methods
       {:get {:produces "text/html"
              :response (fn [ctx] (protected-response ctx))}}

       :access-control
       {:scheme :my-custom-authn
        :authorization {:methods {:get :user}}}})]

    ["/jwt"
     (yada/resource
      {:methods
       {:get {:produces "text/html"
              :response (fn [ctx] (protected-response ctx))}}

       :access-control
       {:scheme :my-jwt-scheme
        :authorization {:methods {:get :user}}}})]]])
