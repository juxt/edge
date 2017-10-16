;; Copyright © 2016, JUXT LTD.

;; A complete development environment for websites in Clojure and
;; ClojureScript.

;; Most users will use 'boot dev' from the command-line or via an IDE
;; (e.g. CIDER).

;; See README.md for more details.

(require '[clojure.java.shell :as sh])

(defn next-version [version]
  (when version
    (let [[a b] (next (re-matches #"(.*?)([\d]+)" version))]
      (when (and a b)
        (str a (inc (Long/parseLong b)))))))

(defn deduce-version-from-git
  "Avoid another decade of pointless, unnecessary and error-prone
  fiddling with version labels in source code."
  []
  (let [[version commits hash dirty?]
        (next (re-matches #"(.*?)-(.*?)-(.*?)(-dirty)?\n"
                          (:out (sh/sh "git" "describe" "--dirty" "--long" "--tags" "--match" "[0-9].*"))))]
    (cond
      dirty? (str (next-version version) "-" hash "-dirty")
      (pos? (Long/parseLong commits)) (str (next-version version) "-" hash)
      :otherwise version)))

(def project "edge")
(def version (deduce-version-from-git))

(set-env!
 ;; It's okay for "test" to be used in source-paths as they don't go into
 ;; resulting jar unless AOT'd.
 :source-paths #{"sass" "src" "test"}
 :resource-paths #{"resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "2.0.0" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server
   [deraen/boot-sass "0.3.1" :scope "test"]
   [reloaded.repl "0.2.4" :scope "test"]

   [org.clojure/clojure "1.9.0-alpha17"]
   [org.clojure/clojurescript "1.9.946"]

   [org.clojure/tools.nrepl "0.2.13"]

   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.2" :scope "test"]

   ;; Server deps
   [aero "1.1.2"]
   [bidi "2.1.1"]
   [com.stuartsierra/component "0.3.2"]
   [hiccup "1.0.5"]
   [org.clojure/tools.namespace "0.2.11"]
   [prismatic/schema "1.1.6"]
   [selmer "1.10.8"]
   [yada "1.2.6" :exclusions [ring-swagger]]
   ;; https://github.com/juxt/yada/pull/181
   [org.clojure/core.async "0.3.443"]
   [aleph "0.4.3"]
   [metosin/ring-swagger "0.24.0"]

   ;; App deps
   [reagent "0.7.0"]
   [com.cognitect/transit-clj "0.8.300"]

   ;; Logging
   [org.clojure/tools.logging "0.4.0"]
   [org.slf4j/jcl-over-slf4j "1.7.25"]
   [org.slf4j/jul-to-slf4j "1.7.25"]
   [org.slf4j/log4j-over-slf4j "1.7.25"]
   [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-sass :refer [sass]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl)

(def repl-port 5600)

(task-options!
 repl {:client true
       :port repl-port}
 pom {:project (symbol project)
      :version version
      :description "A complete Clojure project you can leap from"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
 aot {:namespace #{'edge.main}}
 jar {:main 'edge.main
      :file (str project "-app.jar")})

(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (let [run? (atom false)]
    (with-pass-thru _
      (when-not @run?
        (reset! run? true)
        (require 'reloaded.repl)
        (let [go (resolve 'reloaded.repl/go)]
          (try
            (require 'user)
            (go)
            (catch Exception e
              (boot.util/fail "Exception while starting the system\n")
              (boot.util/print-ex (.getCause e)))))))))

(deftask dev
  "This is the main development entry point."
  []
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (speak)
   (sass :output-style :expanded)
   (reload :on-jsload 'edge.main/init)
   (dev-system)
   ; this is also the server repl!
   (cljs-repl :nrepl-opts {:client false
                           :port repl-port
                           :init-ns 'user})
   (cljs :optimizations :none)
   (target)))

(deftask static
  "This is used for creating optimized static resources under static"
  []
  (comp
   (sass :output-style :compressed)
   (cljs :optimizations :advanced)))

(deftask build
  []
  (comp
   (static)
   (target :dir #{"static"})))

(deftask run-system
  [p profile VAL kw "Profile to start system with"]
  (require 'edge.system)
  (let [new-system (resolve 'edge.system/new-system)]
    (with-pre-wrap fileset
      (let [system (new-system profile)]
        (component/start system)
        (intern 'boot.user 'system system)
        (assoc fileset :system system)))))

(deftask run
  [p profile VAL kw "Profile"]
  (comp
   (repl :server true
         :port (case profile :prod 5601 :beta 5602 5600))
   (run-system :profile (or profile :prod))
   (wait)))

(deftask uberjar
  "Build an uberjar"
  []
  (println "Building uberjar")
  (comp
   (static)
   (aot)
   (pom)
   (uber)
   (jar)
   (target)))

(deftask show-version "Show version" [] (println version))
