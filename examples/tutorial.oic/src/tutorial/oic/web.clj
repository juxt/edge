(ns tutorial.oic.web
  (:require
   [aleph.http :as http]
   [integrant.core :as ig]
   [manifold.deferred :as d]
   [ring.util.codec :as codec]
   [yada.yada :as yada]

   ;; Note: this may be promoted to yada 1.3.x or a separate Edge
   ;; module
   [tutorial.oic.oic :as oic]

   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::authorize [_ {:keys [tutorial.oic.oic/*client]}]
  (yada/resource
   {:methods
    {:get
     {:produces "text/plain"
      :response
      (fn [ctx]
        (d/let-flow [client *client
                     url (:openid-connect/authorization-endpoint client)
                     client-id (:oauth/client-id client)
                     redirect-uri (:oauth/redirect-uri client)]
          (-> ctx
              (yada/redirect
               (str
                url "?"
                (codec/form-encode
                 {"response_type" "code"
                  "client_id" client-id
                  "redirect_uri" redirect-uri
                  "scope" "openid profile permissions"}))))))}}}))

(defmethod ig/init-key ::oauth-callback [_ {:keys [tutorial.oic.oic/*client]}]
  (yada/resource
   {:methods
    {:get
     {:parameters {:query {:code String}}
      :produces "text/plain"
      :response
      (fn [ctx]
        (->
         (d/let-flow [code (get-in ctx [:parameters :query :code])
                      client *client
                      token-url (:openid-connect/token-endpoint client)
                      client-id (:oauth/client-id client)
                      client-secret (:oauth/client-secret client)
                      redirect-uri (:oauth/redirect-uri client)

                      callback-response
                      (http/post
                       token-url
                       {:accept "application/json"
                        :form-params
                        {"grant_type" "authorization_code"
                         "client_id" client-id
                         "client_secret" client-secret
                         "code" code
                         "redirect_uri" redirect-uri}})

                      decoded (oic/validate-callback-response
                               callback-response
                               (:openid-connect/jwks client))]

           ;; We could now set a cookie (session id or signed JWT
           ;; token) to prove that these claims are established for
           ;; future request.

           ;; We might also redirect appropriately.

           ;; For the purposes of this tutorial, let's just print the
           ;; user's name.
           (str "Your name is " (get-in decoded [:claims "name"])))

         ;; Error handling
         (d/catch
             (fn [e]
               (log/errorf e "Error in OAuth2 callback occured: %s" (.getMessage e))
               (d/error-deferred (throw (ex-info "Failed to authorize" {:status 500} e)))
               ))))}}}))
