(ns ^{:clojure.tools.namespace.repl/load false}
    nrepl
    (:require
     [clojure.tools.nrepl.server :as nrepl.server]
     [cider.nrepl]
     [cemerick.piggieback]
     [refactor-nrepl.middleware :as refactor.nrepl]))

(defn start-nrepl
  []
  (let [server
        (nrepl.server/start-server
          :handler
          (apply nrepl.server/default-handler

                 (conj (map #'cider.nrepl/resolve-or-fail cider.nrepl/cider-middleware)
                       #'refactor.nrepl/wrap-refactor
                       #'cemerick.piggieback/wrap-cljs-repl
                       )))]
    (spit "../.nrepl-port" (:port server))
    (println "NREPL port:" (:port server))
    server))

(def server (start-nrepl))
