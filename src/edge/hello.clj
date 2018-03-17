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

(defn hello-atom []
  ["/hello-atom"
   (yada/as-resource (atom "Hello World!\n"))])

(defn hello-parameter []
  ["/hello-parameter"
   (yada/resource
    {:methods
     {:get
      {:parameters {:query {:p String}}
       :produces "text/plain"
       :response (fn [ctx] (format "Hello %s!\n" (-> ctx :parameters :query :p)))
       }}})])

(defn other-hello-routes []
  ["" [
       (hello-language)
       (hello-atom)
       (hello-parameter)
       ]])
