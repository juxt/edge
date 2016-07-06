;; Copyright © 2016, JUXT LTD.

;; A complete development environment for websites in Clojure and
;; ClojureScript.

;; Most users will use 'boot dev' from the command-line or via an IDE
;; (e.g. CIDER).

;; See README.md for more details.

(set-env!
 :source-paths #{"sass" "src"}
 :resource-paths #{"resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.2" :scope "test"]
   [adzerk/boot-reload "0.4.11" :scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server
   [deraen/boot-sass "0.2.0" :scope "test"]
   [reloaded.repl "0.2.1" :scope "test"]

   [org.clojure/clojure "1.8.0"]
   [org.clojure/clojurescript "1.9.14"]

   [org.clojure/tools.nrepl "0.2.12"]

   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]

   ;; Server deps
   [aero "1.0.0-beta5"]
   [aleph "0.4.1"]
   [bidi "2.0.9"]
   [com.stuartsierra/component "0.3.1"]
   [hiccup "1.0.5"]
   [org.clojure/tools.namespace "0.2.11"]
   [prismatic/schema "1.0.4"]
   [selmer "1.0.4"]
   [yada "1.1.28"]

   ;; App deps
   [reagent "0.6.0-rc"]
   [com.cognitect/transit-clj "0.8.285"]
   ;;[com.cognitect/transit-cljs "0.8.239"]

   ;; Logging
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.18"]
   [org.slf4j/jul-to-slf4j "1.7.18"]
   [org.slf4j/log4j-over-slf4j "1.7.18"]
   [ch.qos.logback/logback-classic "1.1.5"
    :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-sass :refer [sass]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[edge.system :refer [new-system]])

(def repl-port 5600)

(task-options!
 repl {:client true
       :port repl-port})

(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (require 'reloaded.repl)
  (let [go (resolve 'reloaded.repl/go)]
    (try
      (require 'user)
      (go)
      (catch Exception e
        (boot.util/fail "Exception while starting the system\n")
        (boot.util/print-ex e))))
  identity)

(deftask dev
  "This is the main development entry point."
  []
  (set-env! :dependencies #(vec (concat % '[[reloaded.repl "0.2.1"]])))
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (speak)
   (sass :output-style :expanded)
   (reload :on-jsload 'edge.main/init)
   (cljs-repl :nrepl-opts {:client false
                           :port repl-port
                           :init-ns 'user}) ; this is also the server repl!
   (cljs :ids #{"edge"} :optimizations :none)
   (dev-system)
   (target)))

(deftask build
  "This is used for creating optimized static resources under static"
  []
  (comp
   (sass :output-style :compressed)
   (cljs :ids #{"edge"} :optimizations :advanced)
   (target :dir #{"static"})))

(defn- run-system [profile]
  (println "Running system with profile" profile)
  (let [system (new-system profile)]
    (component/start system)
    (intern 'user 'system system)
    (with-pre-wrap fileset
      (assoc fileset :system system))))

(deftask run [p profile VAL kw "Profile"]
  (comp
   (repl :server true
         :port (case profile :prod 5601 :beta 5602 5600)
         :init-ns 'user)
   (run-system (or profile :prod))
   (wait)))
