;; Copyright © 2016-2018, JUXT LTD.
(ns dev-extras
  (:require
   [clojure.test :refer [run-all-tests]]
   [edge.system :as system]
   [integrant.repl]
   [integrant.repl.state]
   io.aviso.ansi))

(defmacro ^:private proxy-ns
  [ns & vars]
  (cons `do
        (map (fn [v] `(do
                        (def ~v ~(symbol (str ns) (str v)))
                        (alter-meta!
                          (resolve '~v)
                          (constantly (meta (resolve '~(symbol (str ns) (str v)))))))) vars)))
 
(proxy-ns integrant.repl clear halt prep init reset reset-all)

(def system integrant.repl.state/system)

(add-watch #'integrant.repl.state/system
           ::system-watcher
           (fn [_ _ _ new]
             (alter-var-root #'system (constantly new))))

(defn go []
  (let [res (integrant.repl/go)]
    (println (io.aviso.ansi/yellow
               (format "[Edge] Website ready: %s"
                       (-> system :edge/web-listener :config))))
    (println (io.aviso.ansi/bold-yellow "[Edge] Now make code changes, then enter (reset) here"))
    res))

(integrant.repl/set-prep! #(system/system-config {:profile :dev}))

(defn test-all []
  (run-all-tests #"edge.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

(defn cljs-repl
  "Start a ClojureScript REPL"
  []
  (eval
    `(do
       (require 'figwheel-sidecar.repl-api)
       (figwheel-sidecar.repl-api/cljs-repl))))
