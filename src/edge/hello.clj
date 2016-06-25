;; Copyright © 2016, JUXT LTD.

(ns edge.hello
  "Demonstrating a simple example of a yada web resource"
  (:require
   [clojure.tools.logging :refer :all]
   [yada.yada :as yada]))

(defn hello-routes [deps]
  ["/hello" (yada/handler "Hello World!\n")])
