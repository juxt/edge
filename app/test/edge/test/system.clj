;; Copyright © 2016, JUXT LTD.

(ns edge.test.system
  (:require
   [integrant.core :as ig]))

(def ^:dynamic *system* nil)

(defmacro with-system
  [system & body]
  `(let [s# (ig/init ~system)]
     (try
       (binding [*system* s#]
         ~@body)
       (finally
         (ig/halt! s#)))))

(defn with-system-fixture
  [system]
  (fn [f]
    (with-system (system)
      (f))))
