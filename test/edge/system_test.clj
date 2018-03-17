;; Copyright © 2016, JUXT LTD.

(ns edge.system-test
  (:require
   [clojure.test :refer :all :exclude [deftest]]
   [com.stuartsierra.component :refer [system-using system-map]]
   [edge.test.system :refer [with-system-fixture *system*]]
   [schema.test :refer [deftest]]
   [yada.test :refer [response-for]])
  (:import [com.stuartsierra.component SystemMap]))

(defn new-system
  "Define a minimal system which is just enough for the tests in this
  namespace to run"
  []
  (system-using
   (system-map)
   {}))

(use-fixtures :once (with-system-fixture new-system))

(deftest system-test
  (is *system*)
  (is (instance? SystemMap *system*)))

