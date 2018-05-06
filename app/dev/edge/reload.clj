(ns edge.reload
  (:require
   [integrant.repl :refer [reset]]))

(defn reset-on-hup []
  (let [bindings (get-thread-bindings)]
    (sun.misc.Signal/handle
      (sun.misc.Signal. "HUP")
      (reify sun.misc.SignalHandler
        (handle [_ signal]
          (with-bindings bindings
            (println (reset))))))))
