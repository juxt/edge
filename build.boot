(require '[boot.pod :as pod])

(deftask server
  "Develop the server backend"
  []
  (boot.core/with-pass-thru [_]
    (let [pod (pod/make-pod
                 (-> (get-env)
                     (assoc :dependencies '[[aleph "0.4.1-beta3"]
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
                                            [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]

                                            [org.clojure/tools.namespace "0.2.10"]]
                            :source-paths ["src" "resources" "dev"])))]

        (pod/with-eval-in pod
          (require 'boot.repl)
          (boot.repl/launch-nrepl {:init-ns 'user
                                   :port 5700
                                   :default-middleware @boot.repl/*default-middleware*
                                   :default-dependencies @boot.repl/*default-dependencies*})))))

(deftask frontend
  "Simple alias to run frontend application"
  []
  (set-env! :target-path "target/dev"
            :source-paths #{"sass" "src" "resources" "dev-cljs"}
            :asset-paths #{"assets"}
            :dependencies '[[org.omcljs/om "1.0.0-alpha28"]
                            [adzerk/boot-cljs "1.7.48-5" :scope "test"]
                            [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                            [adzerk/boot-reload "0.4.0" :scope "test"]
                            [deraen/boot-less "0.5.0"]
                            [mathias/boot-sassc "0.1.5" :scope "test"]
                            [org.clojure/clojure "1.8.0"]
                            [org.clojure/clojurescript "1.7.170"]
                            [org.clojure/tools.nrepl "0.2.12"]
                            [org.clojure/core.async "0.2.374"]
                            [org.clojure/tools.logging "0.3.1"]
                            [org.slf4j/jcl-over-slf4j "1.7.13"]
                            [org.slf4j/jul-to-slf4j "1.7.13"]
                            [org.slf4j/log4j-over-slf4j "1.7.13"]
                            [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
                            [com.cemerick/piggieback "0.2.1" :scope "test"]
                            [weasel "0.7.0" :scope "test"]])

  (require '[adzerk.boot-cljs :refer [cljs]]
           '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
           '[adzerk.boot-reload :refer [reload]]
           '[boot.pod :as pod]
           '[clojure.java.shell :as sh]
           '[mathias.boot-sassc :refer [sass]]
           '[deraen.boot-less :refer [less]])

  (let [reload (resolve 'adzerk.boot-reload/reload)
        cljs-repl (resolve 'adzerk.boot-cljs-repl/cljs-repl)
        cljs (resolve 'adzerk.boot-cljs/cljs)
        less (resolve 'deraen.boot-less/less)
        sass (resolve 'mathias.boot-sassc/sass)]

    (comp
     (watch)
     (speak)
     #_(sass :sass-file "app.scss"
           :output-dir "."
           :line-numbers true
           :source-maps true)
     (less :source-map true)
     (reload :on-jsload 'edge.main/init)
     (cljs-repl :nrepl-opts {:port 5710})
     (cljs :ids #{"edge"} :optimizations :none)
     (target :dir #{"target/dev"}))))

#_(deftask build-frontend
 "Compiles frontend application"
 []
 (set-env! :target-path "target/prod")
 (comp
  (less :source-map false)
  (cljs :ids #{"edge"} :optimizations :advanced)
  (target :dir #{"target/prod"})))
