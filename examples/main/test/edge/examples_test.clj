;; Copyright © 2017, JUXT LTD.

(ns edge.examples-test
  (:require
   [buddy.sign.jwt :as jwt]
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
    (is (= 200 (:status response)))
    (testing "Absence of authenticate header"
      (is (nil? (get-in response [:headers "www-authenticate"]))))))

(deftest custom-auth-trusted-header-test
  (let [response (yada/response-for custom-auth-trusted-header-resource-example :get "/" {})]
    (is (= 401 (:status response)))
    (testing "Absence of authenticate header"
      (is (nil? (get-in response [:headers "www-authenticate"]))))
    (let [response (yada/response-for custom-auth-trusted-header-resource-example :get "/" {:headers {"x-whoami" "bob"}})]
      (is (= 200 (:status response))))))

(defn sign [creds]
  (jwt/sign {:claims (pr-str creds)} secret))

(def alice-sig "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbXMiOiJ7OnVzZXIgXCJhbGljZVwiLCA6cm9sZXMgI3s6dXNlcn19In0.yMoqLM3zPkej-W6CoEaJ7GWxxrsbEiYa_yiRw7rPDmU")

(def dave-sig "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbXMiOiJ7OnVzZXIgXCJkYXZlXCIsIDpyb2xlcyAje319In0.Kd90HYTQFDC-ZAC95CvrnY1PWQXGBkLMOzS2lnC9ZA8")

(deftest secrets-test []
  (is (= alice-sig (sign {:user "alice" :roles #{:user}})))
  (is (= dave-sig (sign {:user "dave" :roles #{}}))))

(deftest custom-auth-signed-header-test
  (let [response (yada/response-for custom-auth-signed-header-resource-example :get "/" {})]
    (testing "No authentication given"
      (is (= 401 (:status response)))
      (testing "Absence of authenticate header"
        (is (nil? (get-in response [:headers "www-authenticate"])))))
    (testing "Alice"
      (let [response (yada/response-for custom-auth-signed-header-resource-example :get "/" {:headers {"x-whoami" alice-sig}})]
        (is (= 200 (:status response)))))
    (testing "Dave is forbidden with 403."
      (let [response (yada/response-for custom-auth-signed-header-resource-example :get "/" {:headers {"x-whoami" dave-sig}})]
        (is (= 403 (:status response)))))))
