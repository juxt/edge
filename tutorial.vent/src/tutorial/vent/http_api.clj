(ns tutorial.vent.http-api
  (:require
    [integrant.core :as ig]
    [yada.yada :as yada]
    yada.handler
    [yada.security :refer [verify]]
    [clojure.walk :as walk]))

(defn- resolve-params
  [m pull]
  (reduce-kv
    (fn [pulled k v]
      (assoc pulled k (get-in m v)))
    pull
    pull))

(defn- api-resource
  [res]
  (yada.handler/insert-interceptor
    (yada/handler
      (yada/resource
        (-> res
            (update :methods
                    (fn [methods]
                      (reduce-kv
                        (fn [methods k v]
                          (let [sym (::symbol v)]
                            (require (symbol (namespace sym)))

                            (-> (walk/postwalk
                                  (fn [x]
                                    (if (= 'String x)
                                      String
                                      x))
                                  methods)

                                (update-in [k :produces]
                                           (fnil conj #{})
                                           "application/edn")
                                (assoc-in [k :response]
                                          (fn [ctx]
                                            (def ctx ctx)
                                            (if-let [params (::params v)]
                                              ((resolve sym)
                                               (resolve-params ctx params))
                                              ((resolve sym))))))))
                        methods
                        methods))))))
    yada.security/authorize
    (fn [ctx]
      (assoc ctx :authentication "developer"))))

(defmethod ig/init-key ::api-resource
  [_ res]
  (api-resource res))
