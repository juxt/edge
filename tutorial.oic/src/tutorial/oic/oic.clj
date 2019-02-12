(ns tutorial.oic.oic
  (:require
   [aleph.http :as http]
   [buddy.core.keys :as bck]
   [buddy.sign.jws :as jws]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [manifold.deferred :as d]))

(defn- b64-decode-json-str
  "Decode a base-64 encoded JSON string. (UTF-8 assumed, no BOM check)."
  [s] nil
  (try
    (json/decode (String. (.decode (java.util.Base64/getUrlDecoder) s)))
    (catch Exception e
      (throw (ex-info "Failed to decode string" {:string s} e)))))

(defn- decode-jwt
  "Transform a properly formed JWT into a Clojure map. From
  https://gist.github.com/raymcdermott/1f38ec455df433b96da789a70a4dd346"
  [jwt]
  (when-let [jwt-parts (string/split jwt #"\.")]
    (when (= 3 (count jwt-parts))
      (let [[b64-header b64-payload b64-signature] jwt-parts]
        {:header (b64-decode-json-str b64-header)
         :payload (b64-decode-json-str b64-payload)
         :signature b64-signature}))))

(defn- cert->pem
  "Convert cert to PEM. From
  https://gist.github.com/raymcdermott/1f38ec455df433b96da789a70a4dd346"
  [cert]
  (bck/str->public-key
   (str "-----BEGIN CERTIFICATE-----\n"
        (string/join "\n" (string/join "\n" (re-seq #".{1,64}" cert)))
        "\n-----END CERTIFICATE-----\n")))

(defn validate-callback-response [response jwks]

  (when-not (= (:status response) 200)
    (throw (ex-info "Callback response did not return OK status" {})))

  (let [body (json/parse-stream (io/reader (:body response)))
        id-token (get body "id_token")
        expires-in (get body "expires_in") ; TODO: use this!
        jwt (decode-jwt id-token)
        issuer (get-in jwt [:payload "iss"])
        kid (get-in jwt [:header "kid"])
        signing-key (first (filter #(= kid (get % "kid")) (get jwks "keys")))
        cert (first (get signing-key "x5c"))
        claims (json/decode
                (String.
                 (jws/unsign
                  id-token
                  (cert->pem cert)
                  {:alg (keyword (string/lower-case (get signing-key "alg")))})
                 "UTF-8"))]

    ;; TODO: ID Token Validation
    ;; See https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation

    {:issuer issuer
     :expires-in expires-in
     :claims claims
     :jwt jwt}))

(defn jwks [config-url]
  (d/let-flow
      [config-response (http/get config-url)
       config (-> config-response :body io/input-stream io/reader json/decode-stream)
       token-endpoint (get config "token_endpoint")
       jwks-uri (get config "jwks_uri")
       jwks-response (http/get jwks-uri)
       jwks (-> jwks-response :body io/input-stream io/reader json/decode-stream)]

    {:openid-connect/jwks jwks
     :openid-connect/authorization-endpoint (get config "authorization_endpoint")
     :openid-connect/token-endpoint (get config "token_endpoint")}))

(defmethod ig/init-key ::*client [_ v]
  ;; Returns a deferred value, containing client configuration
  (d/let-flow [jwks (jwks (:openid-connect/configuration-url v))]
    (merge v jwks)))
