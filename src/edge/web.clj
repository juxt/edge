;; Copyright © 2016, JUXT LTD.

(ns edge.web
  "URI Routes to web content"
  (:require
   [clojure.tools.logging :refer :all]
   [yada.yada :refer [resource handler]]))

(defn routes [deps]
  (fn []
    ["/"
     [
      ["hello.html" (handler "Hello")]
      ["goodbye.html" (handler "Goodbye")]
      ]]))





