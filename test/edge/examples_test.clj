;; Copyright © 2017, JUXT LTD.

(ns edge.examples-test
  (:require
   [yada.yada :as yada]
   [ring.util.codec :as codec]
   [clojure.test :refer :all]
   [edge.examples :refer :all]))

(deftest basic-authentication-test
  (let [response (yada/response-for basic-resource :get "/" {})]
    (is (= 401 (:status response)))
    (is (= ["Basic realm=\"default\""] (get-in response [:headers "www-authenticate"]))))

  (let [response (yada/response-for basic-resource :get "/" {:headers {"authorization" (str "Basic " (codec/base64-encode (.getBytes "alice:password")))}})]
    (is (= 200 (:status response)))))
