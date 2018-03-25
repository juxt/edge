;; Copyright © 2016, JUXT LTD.

(ns edge.system-test
  (:require
   [clojure.test :refer :all :exclude [deftest]]
   [integrant.core :as ig]
   [edge.test.system :refer [with-system-fixture *system*]]
   [schema.test :refer [deftest]]
   [yada.test :refer [response-for]]))

(defn new-system
  "Define a minimal system which is just enough for the tests in this
  namespace to run"
  []
  {})

(use-fixtures :once (with-system-fixture new-system))

(deftest system-test
  (is *system*)
  (is (= {} *system*)))
