;; Copyright © 2016, JUXT LTD.

(set-env!
 :target-path "target/dev"
 :source-paths #{"sass" "src" "resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[io.dominic/boot-cljs "1.7.228-3" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
   [adzerk/boot-reload "0.4.0" :scope "test"]
   [deraen/boot-less "0.5.0"]
   [mathias/boot-sassc "0.1.5" :scope "test"]
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

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[boot.pod :as pod]
         '[clojure.java.shell :as sh]
         '[mathias.boot-sassc :refer [sass]]
         '[io.dominic.boot-snippets :refer [with-env]])

(def server-deps
  '[[aleph "0.4.1-beta3"]
    [bidi "1.24.0"]
    [hiccup "1.0.5"]
    [org.omcljs/om "1.0.0-alpha28"]
    [yada "1.1.0-20160126.014942-13"]

    [com.stuartsierra/component "0.3.1"]
    
    [reloaded.repl "0.2.1"]
    [prismatic/schema "1.0.4"]
    [org.clojure/core.async "0.2.374"]
    [org.clojure/tools.reader "0.10.0"]

    [org.clojure/tools.logging "0.3.1"]
    [org.slf4j/jcl-over-slf4j "1.7.13"]
    [org.slf4j/jul-to-slf4j "1.7.13"]
    [org.slf4j/log4j-over-slf4j "1.7.13"]
    [ch.qos.logback/logback-classic "1.1.3"
     :exclusions [org.slf4j/slf4j-api]]

    [org.clojure/tools.namespace "0.2.10"]])

(deftask server
  "Develop the server backend"
  []
  (let [p (pod/make-pod
           {:dependencies (concat server-deps
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
      ;; Auto-start the system
      (require 'dev) ;; Set the init-system (stateful at namespace level)
      (require 'reloaded.repl)
      (reloaded.repl/go))
    identity)) ;; Return identity so that composition works

(deftask frontend
  "Simple alias to run frontend application"
  []
  (let [reload (resolve 'adzerk.boot-reload/reload)
        cljs-repl (resolve 'adzerk.boot-cljs-repl/cljs-repl)
        cljs (resolve 'adzerk.boot-cljs/cljs)
        cljs-build-deps (resolve 'adzerk.boot-cljs/deps)
        sass (resolve 'mathias.boot-sassc/sass)

        ;; Front-end dependencies
        cljs-deps '[[org.omcljs/om "1.0.0-alpha28"]
                    [org.clojure/core.async "0.2.374"]]

        remove-unneeded-deps
        (fn [deps]
          ;; # Reasons to keep each:
          ;; - adzerk/boot-cljs     The impl stuff needs this as a dep
          ;; - adzerk/boot-reload   Reloading requires that the cljs namespace of this is pulled in
          ;; - weasel               Websocket client
          (filter (comp #{'weasel 'adzerk/boot-cljs 'adzerk/boot-reload 'io.dominic/boot-cljs 'org.clojure/clojurescript} first) deps))
        cljs-env
        {:dependencies (-> (boot.core/get-env :dependencies)
                           remove-unneeded-deps
                           (concat @@(resolve 'adzerk.boot-cljs/deps))
                           (concat cljs-deps)
                           vec)
         :directories #{"src" "resources"}}]
    (comp
     (watch)
     (speak :theme "ordinance")
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
     (target :dir #{"target/dev"}))))

(deftask dev
  []
  (comp
   (server)
   (frontend)))


