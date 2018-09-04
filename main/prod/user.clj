(ns user
  (:gen-class)
  (:require
   [clojure.tools.namespace.repl :as repl]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [edge.system :refer [system-config]]))

(repl/disable-reload!)

(when (System/getProperty "edge.load_nrepl")
  (require 'nrepl))

(def system {})

(defonce system-agent (agent {}))

(defn resume [profile]
  (println "Resuming system")
  (ig/init (system-config profile)))

(defn reset* []
  (log/info "Signal HUP received, halting!")
  (ig/halt! system)
  (log/info "System halted. Refreshing code")
  (repl/refresh)
  (log/info "Code refreshed. Resuming system.")
  ;; TODO: parameterize profile
  (resume :prod))

(defn reset
  ([]
   (let [thread-bindings (get-thread-bindings)]
     (send
       system-agent
       (fn [system]
         (with-bindings thread-bindings
           (reset*))))))
  ([thread-bindings]
   (send
     system-agent
     (fn [system]
       (with-bindings thread-bindings
         (reset*))))))

(defn -main
  "Run the system. The first argument is the configuration profile (as a
  string), defaulting to 'prod'."
  [& args]

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
            (reset bindings)))))))
