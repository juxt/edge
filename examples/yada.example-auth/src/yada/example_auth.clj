(ns yada.example-auth
  (:require
   [clojure.spec.alpha :as s]
   [integrant.core :as ig]
   [yada.yada :as yada]))

(s/def ::name string?)
(s/def ::password string?)

(defn basic-auth-example-resource [users]
  ;; MAL->DMC: Could we enable spec asserts in the dev alias?
  (s/assert (s/map-of string? (s/keys :req [::name ::password])) users)
  (yada/resource
   {:access-control
    {:realms
     {"default"
      {:authentication-schemes
       [{:scheme "Basic"
         :verify
         (fn [[user-id given-password]]
           (when-let [user (get users user-id)]
             (when (= given-password (::password user))
               {::user (dissoc user ::password)})))}]
       :authorization
       {:validate
        (fn [ctx creds] (when creds ctx))}}}}

    :methods
    {:get
     {:produces
      {:media-type "text/plain" :charset "utf8"}
      :response
      (fn [ctx]
        (format
         "Welcome %s"
         (-> ctx :authentication (get "default") ::user ::name)))}}}))

(defmethod ig/init-key ::basic-auth-example-resource
  [_ {:keys [users]}]
  (basic-auth-example-resource users))
