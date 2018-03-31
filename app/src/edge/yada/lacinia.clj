(ns edge.yada.lacinia
  (:require
   [clojure.tools.logging :refer :all]
   [manifold.deferred :as d]
   [manifold.stream :as ms]
   com.walmartlabs.lacinia.parser
   [com.walmartlabs.lacinia :refer [execute]]
   [com.walmartlabs.lacinia.executor :as executor]
   [com.walmartlabs.lacinia.resolve :as resolve]))

;; TODO Promote to yada/ext
(defn query [db schema q]
  (assert schema)
  (execute schema q nil {:db db}))

;; TODO: Promote to yada/ext
(defn subscription-stream [db schema q]
  (assert schema)
  ;; see com.walmartlabs.lacinia.pedestal.subscriptions/query-parser-interceptor
  (assert (map? schema))

  (let [parsed-query (com.walmartlabs.lacinia.parser/parse-query schema q nil)
        prepared (com.walmartlabs.lacinia.parser/prepare-with-query-variables parsed-query {} #_variables)
        ;; TODO: Validate
        ;;(validator/validate actual-schema prepared {})
        ctx {com.walmartlabs.lacinia.constants/parsed-query-key prepared}]

    (let [cancel (promise)
          source-stream (ms/stream)
          close-fn (com.walmartlabs.lacinia.executor/invoke-streamer
                    ctx (fn callback [value]
                          (let [value
                                (executor/execute-query
                                 (assoc ctx ::executor/resolved-value value))]

                            (resolve/on-deliver!
                             value
                             (fn [result]
                               (println "value delivered is type" (type result))
                               (d/chain
                                (ms/put! source-stream (pr-str result))
                                (fn [put-result]
                                  (when (false? put-result)
                                    (deliver cancel :stream-closed)))))))))

          _ (future (do @cancel
                        (debug "Closing source stream")
                        (ms/close! source-stream)
                        (debug "Calling streamer's close function to shut it down")
                        (close-fn)))]

      source-stream)))
