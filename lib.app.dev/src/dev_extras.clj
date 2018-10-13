;; Copyright © 2016-2018, JUXT LTD.
(ns dev-extras
  (:require
   [clojure.test :refer [run-all-tests]]
   [edge.system :as system]
   [edge.system.meta :as system.meta]
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

(defmacro ^:private watch-var
  [s alias]
  `(do
     (def ~alias ~s)
     (add-watch (var ~s)
                (keyword "dev-extras" ~(name alias))
                (fn [_# _# _# new#]
                  (alter-var-root
                    (var ~alias)
                    (constantly new#))))))

(watch-var integrant.repl.state/system system)
(watch-var integrant.repl.state/config system-config)

(defn go []
  (let [res (integrant.repl/go)]
    (doseq [message (system.meta/useful-infos system-config system)]
      (println (io.aviso.ansi/yellow (format "[Edge] %s" message))))
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
