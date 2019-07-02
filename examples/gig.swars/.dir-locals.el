((nil
  (cider-ns-refresh-before-fn . "dev-extras/suspend")
  (cider-ns-refresh-after-fn  . "dev-extras/resume")
  (cider-repl-init-code . ("(dev)"))
  (cider-clojure-cli-global-options . "-A:dev:build:dev/build")
  (cider-default-cljs-repl . edge)
  (cider-cljs-repl-types . ((edge "(do (require 'dev-extras) ((resolve 'dev-extras/cljs-repl)))")))))
