(ns edge.yada.lacinia
  (:require
   [clojure.tools.logging :as log]
   [manifold.deferred :as d]
   [manifold.stream :as ms]
   com.walmartlabs.lacinia.parser
   [com.walmartlabs.lacinia :refer [execute]]
   [com.walmartlabs.lacinia.executor :as executor]
   [com.walmartlabs.lacinia.resolve :as resolve]))

;; TODO: Promote to yada/ext
(defn subscription-stream [schema q {:keys [edge/executor] :as config}]
  (assert schema)
  ;; see com.walmartlabs.lacinia.pedestal.subscriptions/query-parser-interceptor
  (assert (map? schema))

  (let [parsed-query (com.walmartlabs.lacinia.parser/parse-query schema q nil)
        prepared (com.walmartlabs.lacinia.parser/prepare-with-query-variables parsed-query {} #_variables)
        ;; TODO: Validate
        ;;(validator/validate actual-schema prepared {})
        ctx (merge config {com.walmartlabs.lacinia.constants/parsed-query-key prepared})]

    (let [source-stream (ms/stream 100 nil executor)
          close-fn (com.walmartlabs.lacinia.executor/invoke-streamer
                     ctx (fn callback [value]
                           (let [value
                                 (executor/execute-query
                                   (assoc ctx ::executor/resolved-value value))]

                             (resolve/on-deliver!
                               value
                               (fn [result]
                                 (ms/put! source-stream result))))))]

      ;; If the source-stream we're about to return is ever closed, stop producing data
      (ms/on-closed source-stream close-fn)

      source-stream)))
