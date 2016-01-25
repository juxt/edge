(set-env!
 :source-paths #{"sass" "src" "resources"}
 :asset-paths #{"assets"}
 :dependencies '[[adzerk/boot-cljs "1.7.48-5" :scope "test"]
                 [adzerk/boot-cljs-repl "0.2.0" :scope "test"]
                 [adzerk/boot-reload "0.4.0" :scope "test"]

                 [org.clojure/core.async "0.2.374"]

                 #_[hiccup "1.0.5"]

                 #_[cljsjs/react "0.14.3-0"]
                 #_[reagent "0.5.0"]
                 #_[re-frame "0.4.1"]

                 #_[kibu/pushy "0.3.2"]

                 #_[cljs-http "0.1.37"]
                 #_[pandeiro/boot-http "0.6.3" :scope "test"]
                 #_[clj-http "2.0.0"]

                 #_[mathias/boot-sassc "0.1.5" :scope "test"]
                 [deraen/boot-less "0.5.0"]

                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.170"]

                 #_[org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [org.omcljs/om "1.0.0-alpha28"]

                 #_[cheshire "5.5.0"]
                 #_[stencil "0.5.0"]
                 [org.clojure/tools.nrepl "0.2.12"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload    :refer [reload]]
         '[clojure.java.shell :as sh]
         #_'[mathias.boot-sassc :refer [sass]]
         '[deraen.boot-less :refer [less]])

(deftask dev
  "Simple alias to run application in development mode"
  []
  (set-env! :target-path "target/dev")
  (comp
   ;;(serve :dir "target/dev" :port 3001)
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
