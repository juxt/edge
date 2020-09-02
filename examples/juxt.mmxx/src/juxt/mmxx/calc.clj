;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.calc)

(defn adoc->html [dependencies]
  {:juxt.http/content-type "text/html;charset=utf-8"
   :juxt.http/payload (.getBytes "<h1>transformed!</h1>\n")})
