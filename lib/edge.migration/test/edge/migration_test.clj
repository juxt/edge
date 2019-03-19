(ns edge.migration-test
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    edge.migration
    [rewrite-clj.parser :refer [parse-string]]))

(deftest migrations-run
  (doseq [example-project ["main" "phonebook-api" "phonebook-app" "tutorial.oic" "tutorial.vent" "yada.example-auth"]]
    (let [output (io/resource (str example-project "/output.edn"))]
      (doseq [history (->> (range 1000) ;; 1000 to prevent infinite loops
                           (map inc)
                           (keep #(io/resource (str example-project "/input/" %)))
                           (take-while some?))]
        (is
          (= (edn/read-string (slurp output))
             (edn/read-string
               (edge.migration/migrate-file
                 (parse-string (slurp history))
                 {:lib-dir "../lib/"}))))
        (is
          (= (slurp output)
             (edge.migration/migrate-file
               (parse-string (slurp history))
               {:lib-dir "../lib/"})))))))
