;; Copyright © 2016, JUXT LTD.

(ns edge.test.system
  (:require
   [com.stuartsierra.component :as component]))

(def ^:dynamic *system* nil)

(defmacro with-system
  [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn with-system-fixture
  [system]
  (fn [f]
    (with-system (system)
      (f))))
