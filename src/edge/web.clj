;; Copyright © 2016, JUXT LTD.

(ns edge.web
  "URI Routes to web content"
  (:require
   [clojure.tools.logging :refer :all]
   [yada.yada :refer [resource handler redirect]]))

(defn content-routes [deps]
  (fn []
    ["/"
     [
      ["index.html" (assoc (handler "Index") :id ::index)]
      ["" (redirect ::index)]
      ["hello.html" (handler "Hello")]
      ["goodbye.html" (handler "Goodbye")]
      ]]))





