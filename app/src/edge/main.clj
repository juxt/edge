;; Copyright © 2016-2018, JUXT LTD.

;; tag::ns[]
(ns edge.main
  "Main entrypoint for production instances"
  (:gen-class)
  (:require
   [clojure.tools.namespace.repl :as repl]
   [integrant.core :as ig]
   [edge.system :refer [system-config]]))
;; end::ns[]

(repl/disable-reload!)

(def system {})

(defonce system-agent (agent {}))

(defn resume [profile]
  (println "Resuming system")
  (ig/init (system-config profile)))

(defn -main [& args]

  (set-error-handler!
    system-agent
    (fn [a error]
      (println "ERROR:" error)
      ;; TODO: Restart the agent
      ))

  (add-watch
    system-agent
    :reset
    (fn [k r old new]
      (alter-var-root #'system (constantly new))))

  (let [profile (or (some-> (first args) keyword) :prod)
        system-config (system-config profile)]
    (send system-agent (constantly (ig/init system-config)))

    (let [bindings (get-thread-bindings)]
      (sun.misc.Signal/handle
        (sun.misc.Signal. "HUP")
        (reify sun.misc.SignalHandler
          (handle [_ signal]
            (send
              system-agent
              (fn [system]
                (with-bindings bindings
                  (println "Signal HUP received, halting!")
                  (ig/halt! system)
                  (println "System halted. Refreshing code")
                  (repl/refresh)
                  (println "Code refreshed. Resuming system.")
                  (resume profile))))))))))
