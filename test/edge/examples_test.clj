;; Copyright © 2017, JUXT LTD.

(ns edge.examples-test
  (:require
   [yada.yada :as yada]
   [ring.util.codec :as codec]
   [clojure.test :refer :all]
   [edge.examples :refer :all]))

(deftest basic-authentication-test
  (let [response (yada/response-for basic-auth-resource-example :get "/" {})]
    (is (= 401 (:status response)))
    (is (= ["Basic realm=\"default\""] (get-in response [:headers "www-authenticate"]))))

  (let [response (yada/response-for basic-auth-resource-example :get "/" {:headers {"authorization" (str "Basic " (codec/base64-encode (.getBytes "alice:password")))}})]
    (is (= 200 (:status response)))))

(deftest custom-auth-static-authentication-test
  (let [response (yada/response-for custom-auth-static-resource-example :get "/" {})]
    (is (= 401 (:status response)))
    (testing "Absence of authenticate header"
      (is (nil? (get-in response [:headers "www-authenticate"]))))
    (let [response (yada/response-for custom-auth-static-resource-example :get "/" {:headers {"x-whoami" "bob"}})]
      (is (= 200 (:status response))))))

(deftest custom-auth-trusted-header-test
  (let [response (yada/response-for custom-auth-trusted-header-resource-example :get "/" {})]
    (is (= 401 (:status response)))
    (testing "Absence of authenticate header"
      (is (nil? (get-in response [:headers "www-authenticate"]))))
    (let [response (yada/response-for custom-auth-trusted-header-resource-example :get "/" {:headers {"x-whoami" "bob"}})]
      (is (= 200 (:status response))))))
