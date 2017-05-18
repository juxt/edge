(ns edge.examples
  (:require
   [buddy.sign.jwt :as jwt]
   [clojure.edn :as edn]
   [clojure.tools.logging :refer :all]
   [hiccup.page :refer [html5]]
   yada.bidi
   [yada.yada :as yada]
   [yada.security :refer [verify]]))

(extend-protocol bidi.bidi/Matched
  clojure.lang.Var
  (resolve-handler [this m] (bidi.bidi/resolve-handler @this m))
  (unresolve-handler [this m] (bidi.bidi/unresolve-handler @this m)))

(defn- restricted-content [ctx]
  (html5
   [:body
    [:h1 (format "Hello %s!" (get-in ctx [:authentication "default" :user]))]
    [:p "You're accessing a restricted resource!"]
    [:pre (pr-str (get-in ctx [:authentication "default"]))]]))

;; Basic Authentication --------------------------------------------------------------

(def basic-auth-resource-example
  (yada/resource
   {:id :edge.resources/basic-authn-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme "Basic"
     :verify (fn [[user password]]
               (when (= user "alice")
                 {:user user
                  :roles #{:user}}))
     :authorization {:methods {:get :user}}}}))

;; Custom Authentication -------------------------------------------------------------

(def custom-auth-static-resource-example
  (yada/resource
   {:id :edge.resources/custom-auth-static-resource-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme :edge/custom-static
     :authorization {:methods {:get :user}}}}))

(defmethod verify :edge/custom-static
  [ctx scheme]
  (when-let [user (get-in ctx [:request :headers "x-whoami"])]
    {:user user
     :roles #{:user}}))

;; Customer Authentication (trusted header) -------------------------------------------------------

(def custom-auth-trusted-header-resource-example
  (yada/resource
   {:id :edge.resources/custom-auth-trusted-header-resource-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme :edge/trusted-whoami-header
     :authorization {:methods {:get :user}}}}))

(defmethod verify :edge/trusted-whoami-header
  [ctx scheme]
  (when-let [user (get-in ctx [:request :headers "x-whoami"])]
    {:user user
     :roles #{:user}}))

;; JSON Web Tokens -----------------------------------------------------------------

(def jwt-resource-example
  (yada/resource
   {:methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme :edge/jwt
     :authorization {:methods {:get :user}}}}))

(def secret "ieXai7aiWafeSh6oowow")

(defmethod verify :edge/jwt
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
    ["/basic" #'basic-auth-resource-example]
    ["/custom-static" #'custom-auth-static-resource-example]
    ["/custom-trusted-header" #'custom-auth-trusted-header-resource-example]
    ["/jwt" #'jwt-resource-example]]])
