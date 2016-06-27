;; Copyright © 2016, JUXT LTD.

(ns edge.hello
  "Demonstrating a simple example of a yada web resource"
  (:require
   [yada.yada :as yada]))

(defn hello-routes []
  ["/hello" (yada/handler "Hello World!\n")])

(defn hello-language []
  ["/hello-language"
   (yada/resource
    {:methods
     {:get
      {:produces
       {:media-type "text/plain"
        :language #{"en" "zh-ch;q=0.9"}}
       :response
       #(case (yada/language %)
          "zh-ch" "你好世界\n"
          "en" "Hello World!\n")}}})])

(defn other-hello-routes []
  ["" [
       (hello-language)
       ]])
