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

;; Basic Authentication --------------------------------------------------------------

(def basic-resource
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
     :authorization {:methods {:get :user}}}}))

;; Custom Authentication -------------------------------------------------------------

(defmethod verify :my-custom-authn
  [ctx scheme]
  (when-let [user (get-in ctx [:request :headers "x-whoami"])]
    {:user user
     :roles #{:user}}))

(def custom-resource
  (yada/resource
   {:id :edge.resources/custom-authn-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (protected-response ctx))}}

    :access-control
    {:scheme :my-custom-authn
     :authorization {:methods {:get :user}}}}))

;; JSON Web Tokens -----------------------------------------------------------------

(def jwt-resource
  (yada/resource
   {:methods
    {:get {:produces "text/html"
           :response (fn [ctx] (protected-response ctx))}}

    :access-control
    {:scheme :my-jwt-scheme
     :authorization {:methods {:get :user}}}}))

(def secret "ieXai7aiWafeSh6oowow")

(defmethod verify :my-jwt-scheme
  [ctx scheme]
  (some->
   (get-in ctx [:request :headers "x-whoami"])
   (jwt/unsign secret)
   :claims
   edn/read-string))

;; Session Tokens -----------------------------------------------------------------

;; TODO

;; Forms -----------------------------------------------------------------

;; TODO

;; Route structure

(defn authentication-example-routes []
  ["/authn-examples"
   [
    ["/basic" basic-resource]
    ["/custom" custom-resource]
    ["/jwt" jwt-resource]]])
