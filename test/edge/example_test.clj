(ns edge.example-test
  (:require
   [clojure.test :refer :all :exclude [deftest]]
   [com.stuartsierra.component :refer [system-using system-map]]
   [edge.test.system :refer [with-system-fixture *system*]]
   [schema.test :refer [deftest]])
  (:import [com.stuartsierra.component SystemMap]))

(defn new-system
  "Define a minimal system which is just enough for the tests in this
  namespace to run"
  []
  (system-using
   (system-map)
   {}))

(use-fixtures :each (with-system-fixture new-system))

(deftest example-test
  (is (= 4 (+ 2 2)))
  (is *system*)
  (is (instance? SystemMap *system*)))

