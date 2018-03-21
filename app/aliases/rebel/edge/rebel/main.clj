(ns edge.rebel.main
  (:require
    rebel-readline.clojure.main
    rebel-readline.core))

(defn -main
  [& args]
  (rebel-readline.core/ensure-terminal
    (rebel-readline.clojure.main/repl
      :init (fn []
              (try
                (println "[edge] Compiling code, please wait...")
                (require 'dev)
                (in-ns 'dev)
                (catch Exception e
                  (.printStackTrace e)
                  (println "[edge] Failed to require dev, this usually means there was a syntax error. See exception above.")
                  (println "[edge] Please correct it, and run (fixed!) to resume development.")))))))
