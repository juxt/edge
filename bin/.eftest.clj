(require '[eftest.runner :refer [find-tests run-tests]])
(require 'orchard.query)

(let [summary (run-tests
                (find-tests
                  ;; This is slow, but because requiring namespaces is slow
                  (orchard.query/namespaces {:load-project-ns? true
                                             :project? true})))]
  (System/exit (+ (:error summary) (:fail summary))))
