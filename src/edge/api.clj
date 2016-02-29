;; Copyright © 2016, JUXT LTD.

(ns edge.api
  "REST API"
  (:require
   [clojure.tools.logging :refer :all]
   [yada.yada :refer [resource handler]]))

(defn routes [deps]
  (fn []
    ["/"
     [
      ["api" (handler "API")]
      ]]))
