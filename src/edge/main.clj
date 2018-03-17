(ns edge.main
  "Entrypoint for production Uberjars"
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [edge.system :refer [new-system]]))

(def system nil)

(defn -main
  [& args]
  (let [system (new-system :prod)]
    (component/start system))
  ;; All threads are daemon, so block forever:
  @(promise))

