(ns tutorial.moan.http-api
  (:require
    [integrant.core :as ig]
    [yada.yada :as yada]))

(defn- api-resource
  ([sym method]
   (api-resource sym method nil))
  ([sym method method-map]
   (require (symbol (namespace sym)))
   (yada/resource
     {:methods
      (hash-map method
                (merge
                  {:produces #{"application/edn"}
                   :response (fn [ctx]
                               ((resolve sym)))}
                  method-map))})))

(defmethod ig/init-key ::api-resource
  [_ {:keys [sym method method-map]
      :or {method :get}}]
  (api-resource sym method method-map))
