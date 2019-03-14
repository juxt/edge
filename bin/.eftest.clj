(require '[eftest.runner :refer [find-tests run-tests]])
(require 'orchard.query)

(run-tests
  (find-tests
    ;; This is slow, but because requiring namespaces is slow
    (orchard.query/namespaces {:load-project-ns? true
                               :project? true})))

;; Some agent, somewhere
(shutdown-agents)
