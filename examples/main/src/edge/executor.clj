(ns edge.executor
  (:require
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [manifold.executor :as me]))

(defmethod ig/init-key :edge/executor [_ _]
  (log/debug "Starting executor")
  (me/fixed-thread-executor 10))

(defmethod ig/halt-key! :edge/executor [_ e]
  (when e
    (log/debug "Shutting down executor")
    (.shutdownNow e)
    (log/debug "Awaiting executor termination")
    (.awaitTermination e 5 java.util.concurrent.TimeUnit/SECONDS)
    (log/debug "Executor terminated")))
