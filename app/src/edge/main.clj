;; Copyright © 2016-2018, JUXT LTD.

;; tag::ns[]
(ns edge.main
  "Main entrypoint for production instances"
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [edge.system :refer [system-config]]))
;; end::ns[]

(defn -main [& args]
  (let [system-config (system-config :prod)] ; <1>
    (ig/init system-config)) ; <2>
  ;; All threads are daemon, so block forever:
  @(promise))
