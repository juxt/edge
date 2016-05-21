;; Copyright © 2016, JUXT LTD.

(set-env!
 :source-paths #{"sass" "src"}
 :resource-paths #{"resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[io.dominic/boot-cljs "1.7.228-3" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
   [adzerk/boot-reload "0.4.0" :scope "test"]
   [deraen/boot-less "0.5.0" :scope "test"]
   [mathias/boot-sassc "0.1.5" :scope "test"]
   [pandeiro/boot-http "0.7.3" :scope "test"]

   [org.clojure/clojure "1.8.0"]
   [org.clojure/tools.nrepl "0.2.12"]
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.13"]
   [org.slf4j/jul-to-slf4j "1.7.13"]
   [org.slf4j/log4j-over-slf4j "1.7.13"]
   [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [org.clojure/clojurescript "1.7.170"]
   [weasel "0.7.0" :scope "test"];; Websocket Server
   [io.dominic/boot-snippets "0.1.0" :scope "test"]])

(def server-deps
  '[[aleph "0.4.1-beta3"]
    [bidi "2.0.9"]
    [hiccup "1.0.5"]
    [org.omcljs/om "1.0.0-alpha28"]
    [yada "1.1.13"]
    [aero "1.0.0-beta3"]

    [com.stuartsierra/component "0.3.1"]

    [reloaded.repl "0.2.1"]
    [prismatic/schema "1.0.4"]
    [org.clojure/core.async "0.2.374"]
    [org.clojure/tools.reader "0.10.0"]

    [org.clojure/tools.logging "0.3.1"]
    [org.slf4j/jcl-over-slf4j "1.7.13"]
    [org.slf4j/jul-to-slf4j "1.7.13"]
    [org.slf4j/log4j-over-slf4j "1.7.13"]
    [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]

    [org.clojure/tools.namespace "0.2.10"]])

(def frontend-deps
  '[[org.omcljs/om "1.0.0-alpha28"]
    [org.clojure/core.async "0.2.374"]])

(require '[boot.pod :as pod]
         '[io.dominic.boot-snippets :refer [with-env]])

(deftask server
  "Develop the server backend"
  []
  (let [p (pod/make-pod
           {:source-paths #{"src"}
            :resource-paths #{"resources"}
            :dependencies (concat server-deps
                                  @@(resolve 'boot.repl/*default-dependencies*))
            :directories #{"src" "resources" "dev"}
            :middleware @@(resolve 'boot.repl/*default-middleware*)})]

    (pod/with-eval-in p
      (require '[boot.pod :as pod])
      (require '[boot.repl])
      (require '[clojure.tools.namespace.repl :as repl])
      (apply repl/set-refresh-dirs (-> pod/env :directories))

      (boot.repl/launch-nrepl {:init-ns 'user :port 5600 :server true
                               :middleware (:middleware pod/env)})
      (require 'reloaded.repl)
      (try
        ;; Auto-start the system
        (require 'dev) ;; Set the init-system (stateful at namespace level)
        (reloaded.repl/go)
        (catch Exception e
          (boot.util/fail "Exception while starting the system\n")
          (boot.util/print-ex e))))
    identity)) ;; Return identity so that composition works

(deftask frontend
  "Simple alias to run frontend application"
  []
  (require 'adzerk.boot-cljs
           'adzerk.boot-cljs-repl
           'adzerk.boot-reload
           'mathias.boot-sassc)
  (let [reload (resolve 'adzerk.boot-reload/reload)
        cljs-repl (resolve 'adzerk.boot-cljs-repl/cljs-repl)
        cljs (resolve 'adzerk.boot-cljs/cljs)
        cljs-build-deps (resolve 'adzerk.boot-cljs/deps)
        sass (resolve 'mathias.boot-sassc/sass)

        remove-unneeded-deps (fn [deps]
                               ;; # Reasons to keep each:
                               ;; - adzerk/boot-cljs     The impl stuff needs this as a dep
                               ;; - adzerk/boot-reload   Reloading requires that the cljs namespace of this is pulled in
                               ;; - weasel               Websocket client
                               (filter (comp #{'weasel 'adzerk/boot-cljs 'adzerk/boot-reload 'io.dominic/boot-cljs 'org.clojure/clojurescript} first) deps))
        cljs-env {:dependencies (-> (boot.core/get-env :dependencies)
                                    remove-unneeded-deps
                                    (concat @@(resolve 'adzerk.boot-cljs/deps))
                                    (concat frontend-deps)
                                    vec)}]
    (comp
     (watch)
     (speak)
     (with-env
       {:directories #{"sass"}}
       (sass :sass-file "app.scss"
             :output-dir "."
             :line-numbers true
             :source-maps true))
     (reload :on-jsload 'edge.main/init)
     (cljs-repl :nrepl-opts {:port 5710})
     (with-env
       cljs-env
       (cljs :ids #{"edge"} :optimizations :none))
     (if (> @boot.util/*verbosity* 1)
       (show :fileset true)
       identity)
     (target :dir #{"target/dev"}))))

(deftask dev
  []
  (comp
   (server)
   (frontend)))
