(ns edge.main
  "Entrypoint for production Uberjars"
  (:gen-class)
  (:require
    [integrant.core :as ig]
    [edge.system :refer [new-system]]))

(def system nil)

(defn -main
  [& args]
  (let [system (new-system :prod)]
    (ig/init system))
  ;; All threads are daemon, so block forever:
  @(promise))

