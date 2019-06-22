;; Copyright © 2016, JUXT LTD.

(ns edge.test.system
  (:require
   [integrant.core :as ig]
   [edge.system]))

(def ^:dynamic *system* nil)

(defmacro ^:private with-system
  [system & body]
  `(let [s# (ig/init ~system)]
     (try
       (binding [*system* s#]
         ~@body)
       (finally
         (ig/halt! s#)))))

(defn- default-system
  []
  (edge.system/system-config
    {:profile :test}))

(defn with-system-fixture
  ([]
   (with-system-fixture default-system))
  ([system]
   (fn [f]
     (with-system (system) nil
       (f)))))
