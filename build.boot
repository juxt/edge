(set-env!
 :source-paths #{"sass" "src" "resources"}
 :asset-paths #{"assets"}
 :dependencies '[[adzerk/boot-cljs "1.7.48-5" :scope "test"]
                 [adzerk/boot-cljs-repl "0.2.0" :scope "test"]
                 [adzerk/boot-reload "0.4.0" :scope "test"]
                 [deraen/boot-less "0.5.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha28"]
                 [org.clojure/tools.nrepl "0.2.12"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload    :refer [reload]]
         '[clojure.java.shell :as sh]
         '[deraen.boot-less :refer [less]])

(deftask server-repl
  "Override"
  []
  (set-env!
   :dependencies '[[bidi "1.24.0"]]
   :source-paths #{"dev" "src" "resources"})
  (repl :init-ns 'user :server true))

(deftask dev
  "Simple alias to run application in development mode"
  []
  (set-env! :target-path "target/dev")
  (comp
   (watch)
   (speak)
   (less :source-map true)
   (reload :on-jsload 'edge.main/init)
   (cljs-repl)
   (cljs :ids #{"edge"} :optimizations :none)
   (target :dir #{"target/dev"})))

(deftask build []
  (set-env! :target-path "target/prod")
  (comp
   (less :source-map false)
   (cljs :ids #{"edge"} :optimizations :advanced)
   (target :dir #{"target/prod"})))
