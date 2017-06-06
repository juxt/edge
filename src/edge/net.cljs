(ns edge.net
  (:require [cljs.core.async :refer [chan put!]]
            [cljs.reader :as reader]))

(defn sse-chan [uri & [error-fn xform]]
  ;; Subscribe to server sent events (SSE):
  (let [chan (chan 10 xform)
        es (new js/EventSource uri)]
    (.addEventListener
      es "message"
      (fn [ev]
        (when-let [m (cljs.reader/read-string (.-data ev))]
          (put! chan m))))

    (.addEventListener
      es "error"
      (fn [ev]
        (if error-fn
          (error-fn)
          (println "oh noes"))))
    chan))
