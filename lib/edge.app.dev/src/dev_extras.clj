;; Copyright © 2016-2019, JUXT LTD.
(ns ^{:clojure.tools.namespace.repl/load false} dev-extras
  "This namespace provides all of the general-purpose dev utilities used from a
  `dev` namespace.  These vars are all available from `dev`."
  (:require
   [clojure.test :refer [run-tests]]
   [edge.system :as system]
   [edge.system.meta :as system.meta]
   [integrant.repl]
   [integrant.repl.state]
   io.aviso.ansi
   clojure.tools.deps.alpha.repl))

(when (try
        (Class/forName "org.slf4j.bridge.SLF4JBridgeHandler")
        (catch ClassNotFoundException _
          false))
  (eval
    `(do
       (org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
       (org.slf4j.bridge.SLF4JBridgeHandler/install))))

(when (try
        (require 'figwheel.main.logging)
        true
        (catch Throwable _))
  ;; Undo default logger being extremely fine grained in figwheel,
  ;; in order to configure figwheel to delegate to slf4j.
  (let [l @(resolve 'figwheel.main.logging/*logger*)]
    ((resolve 'figwheel.main.logging/remove-handlers) l)
    (.setUseParentHandlers l true)))

(defmacro ^:private proxy-ns
  [ns & vars]
  (cons `do
        (map (fn [v] `(do (def ~v ~(symbol (str ns) (str v)))
                          (alter-meta!
                            (resolve '~v)
                            merge
                            {:original-line (:line (meta (resolve '~v)))}
                            (select-keys (meta (resolve '~(symbol (str ns) (str v))))
                                         [:doc :file :line :column :arglists])
                            (meta ~v))))
             vars)))
 
(proxy-ns integrant.repl
  ^{:doc "Stop the system and clear the system variable."} clear
  ^{:doc "Stop the system, if running"} halt
  prep
  init
  ^{:doc "Suspend the system, reload changed code, and start the system again"} reset
  ^{:doc "Suspend the system, reload all code, and start the system again"} reset-all
  ^{:doc "Like halt, but doesn't completely stop some components.  This makes the components faster to start again, but means they may not be completely stopped (e.g. A web server might still have the port in use)"} suspend)

(proxy-ns clojure.tools.deps.alpha.repl add-lib)

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

(watch-var integrant.repl.state/system ^{:doc "After starting your dev system, this will be the system that was started.  You can use this to get individual components and test them in the repl."} system)
(watch-var integrant.repl.state/config ^{:doc "The :ig/system key used to create `system`"} system-config)

(defn go
  "Start the dev system, and output any useful information about the system
  which was just started.  For example, it will output where to open your
  browser to see the application and link to your figwheel auto-test page."
  []
  (let [res (integrant.repl/go)]
    (doseq [message (system.meta/useful-infos system-config system)]
      (println (io.aviso.ansi/yellow (format "[Edge] %s" message))))
    (println (str (io.aviso.ansi/yellow "[Edge] Now make code changes, then enter ")
                  (io.aviso.ansi/bold-yellow "(reset)")
                  (io.aviso.ansi/yellow " here")))
    res))

(defn resume
  "Like `go`, but works on a system suspended with `suspend`."
  []
  (let [res (integrant.repl/resume)]
    (doseq [message (system.meta/useful-infos system-config system)]
      (println (io.aviso.ansi/yellow (format "[Edge] %s" message))))
    res))

(integrant.repl/set-prep! #(system/system-config {:profile :dev}))

(defn set-prep!
  "Set the opts passed to `aero.core/read-config` for the development system.
  
  Example: `(set-prep! {:profile :dev :features [:in-memory-postgres]})`"
  [aero-opts]
  (integrant.repl/set-prep! #(system/system-config aero-opts)))

(defn- test-namespaces
  []
  (keep (fn [[ns vars]]
          (when (some (comp :test meta) vars) ns))
        (map (juxt identity (comp vals ns-publics))
             (all-ns))))

(defn test-all
  "Run all tests"
  []
  (apply run-tests (test-namespaces)))

(defn reset-and-test
  "Reset the system, and run all tests."
  []
  (reset)
  (time (test-all)))

(defn cljs-repl
  "Start a ClojureScript REPL, will attempt to automatically connect to the
  correct figwheel build.  If not possible, will throw and it should be
  provided as an argument."
  ([]
   ;; ensure system is started - this could be less effectful perhaps?
   (go)
   (if (try
         (require 'figwheel-sidecar.repl-api)
         (catch java.io.FileNotFoundException _
           false))
     (eval
       `(do
          (require 'figwheel-sidecar.repl-api)
          (figwheel-sidecar.repl-api/cljs-repl)))
     (eval
       `(do
          (require 'figwheel.main.api)
          (require 'figwheel.main)
          (require 'figwheel.repl)
          (let [builds# (keys @figwheel.main/build-registry)]
            (if (= (count builds#) 1)
              (binding [figwheel.repl/*server* true]
                (figwheel.main.api/cljs-repl (first builds#)))
              (throw (ex-info "A build must be specified, please call with an argument"
                              {:builds builds#}))))))))
  ([build-id]
   ;; Register build with figwheel
   (go)
   ;; Assume figwheel main
   (eval
     `(do
        (require 'figwheel.main.api)
        (require 'figwheel.repl)
        (binding [figwheel.repl/*server* true]
          (figwheel.main.api/cljs-repl ~build-id))))))
