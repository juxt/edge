(ns edge.event-bus
  (:require
   [integrant.core :as ig]
   [manifold.stream :as ms]
   [manifold.deferred :as d]
   manifold.bus))

(defn new-stream [component]
  (assert (:streams component))
  (let [s (ms/stream)]
    (swap! (:streams component) conj s)
    s))

(defn new-promise [component]
  (assert (:promises component))
  (let [p (promise)]
    (swap! (:promises component) conj p)
    p))

(defmethod ig/init-key :edge.component/event-bus
  [_ _]
  {:manifold/event-bus (manifold.bus/event-bus)
   :streams (atom [])
   :promises (atom [])})

(defmethod ig/halt-key! :edge.component/event-bus [_ {:keys [streams promises]}]
  (when-let [streams @streams]
    (doseq [s streams]
      (when-not (ms/closed? s)
        (println "Closing stream due to stopping EventBus component")
        (ms/close! s))))
  (when-let [promises @promises]
    (doseq [p promises]
      (when-not (realized? p)
        (println "Delivering promise due to stopping EventBus component")
        (deliver p :component-stopped)))))
