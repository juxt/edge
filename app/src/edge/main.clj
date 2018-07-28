(ns edge.main
  "Entrypoint for production Uberjars"
  (:gen-class)
  (:require
    [integrant.core :as ig]
    [edge.system :refer [new-system]]))

(def system nil)

;; tag::main[]
(defn -main
  [& args]
  (let [system (new-system :prod)]
    (ig/init system)) ; <1>
  ;; All threads are daemon, so block forever:
  @(promise))
;; end::main[]
