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

(defmethod verify :edge/custom-static
  [ctx scheme]
  {:user "alice"
   :roles #{:user}})

(def custom-auth-static-resource-example
  (yada/resource
   {:id :edge.resources/custom-auth-static-resource-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme :edge/custom-static
     :authorization {:methods {:get :user}}}}))

;; Customer Authentication (trusted header) -------------------------------------------------------

(defmethod verify :edge/trusted-whoami-header
  [ctx scheme]
  (when-let [user (get-in ctx [:request :headers "x-whoami"])]
    {:user user
     :roles #{:user}}))

(def custom-auth-trusted-header-resource-example
  (yada/resource
   {:id :edge.resources/custom-auth-trusted-header-resource-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}

    :access-control
    {:scheme :edge/trusted-whoami-header
     :authorization {:methods {:get :user}}}}))

;; JWT signatures with Buddy -----------------------------------------------------------------

(def secret "ieXai7aiWafeSh6oowow")

(defmethod verify :edge/signed-whoami-header
  [ctx scheme]
  (some->
   (get-in ctx [:request :headers "x-whoami"])
   (jwt/unsign secret)
   :claims
   edn/read-string))

(def custom-auth-signed-header-resource-example
  (yada/resource
   {:methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}
    :access-control
    {:scheme :edge/signed-whoami-header
     :authorization {:methods {:get :user}}}}))

;; Forms -----------------------------------------------------------------

(defmethod verify :edge/signed-cookie
  [ctx scheme]
  (some->
   (get-in ctx [:cookies "session"])
   (jwt/unsign secret)
   :claims
   edn/read-string))

(def cookie-based-restricted-resource
  (yada/resource
   {:id :edge.resources/restricted-example
    :methods
    {:get {:produces "text/html"
           :response (fn [ctx] (restricted-content ctx))}}
    :access-control
    {:scheme :edge/signed-cookie
     :authorization {:methods {:get :user}}}}))

(def login-resource-example
  (yada/resource
   {:id :edge.resources/login-example
    :methods
    {:get
     {:produces "text/html"
      :response
      (fn [ctx]
        (html5
         [:form {:method :post}
          [:p
           [:label {:for "user"} "User "]
           [:input {:type :text :id "user" :name "user"}]]
          [:p
           [:label {:for "password"} "Password "]
           [:input {:type :password :id "password" :name "password"}]]
          [:p
           [:input {:type :submit}]]]))}
     :post
     {:consumes "application/x-www-form-urlencoded"
      :produces "text/plain"
      :parameters
      {:form {:user String
              :password String}}
      :response
      (fn [ctx]
        (let [{:keys [user password]} (-> ctx :parameters :form)]
          (merge
           (:response ctx)
           (if-not (#{"alice" "dave"} user)
             {:body "Login failed"
              :status 401}
             {:status 303
              :headers {"location" (yada/url-for ctx :edge.resources/restricted-example)}
              :cookies
              {"session"
               {:value
                (jwt/sign
                 {:claims (pr-str {:user user :roles #{:user}})}
                 secret)}}}))))}}}))

;; Route structure

(defn authentication-example-routes []
  ["/authn-examples"
   [
    ["/basic" #'basic-auth-resource-example]
    ["/custom-static" #'custom-auth-static-resource-example]
    ["/custom-trusted-header" #'custom-auth-trusted-header-resource-example]
    ["/custom-signed-header" #'custom-auth-signed-header-resource-example]
    ["/restricted-by-cookie" #'cookie-based-restricted-resource]
    ["/login" #'login-resource-example]]])
