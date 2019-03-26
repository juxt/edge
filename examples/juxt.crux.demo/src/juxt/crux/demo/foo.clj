(ns juxt.crux.demo.foo
  (:require
    crux.api
    [yada.yada :as yada]
    [integrant.core :as ig]))

(def id #uuid "50005565-299f-4c08-86d0-b1919bf4b7a9")

(defmethod ig/init-key ::read-write
  [_ {:keys [system]}]
  (yada/resource
    {:id ::read-write
     :methods
     {:get
      {:produces ["text/html" "application/edn" "application/json"]
       :response (fn [ctx]
                   (let [db (crux.api/db system)]
                     (map
                       #(crux.api/entity db (first %))
                       (crux.api/q
                         db
                         {:find '[?e]
                          :where [['?e :crux.db/id id]]}))))}
      :post
      {:produces "text/plain"
       :consumes "application/edn"
       :response
       (fn [ctx]
         (crux.api/submit-tx
           system
           [[:crux.tx/put id
             (merge {:crux.db/id id} (:body ctx))]])
         (yada/redirect ctx ::read-write))}}}))

;; To populate data using cURL:
; $ curl -H "Content-Type: application/edn" -d '{:foo/username "Bart"}' localhost:8300
