(ns tutorial.moan.reload
  (:require
    cljs.repl
    [integrant.core :as ig]
    figwheel.repl))

(defn frontend
  []
  (when-let [repl-env (resolve 'figwheel.main.api/repl-env)]
    (when-let [env (repl-env "app")]
      (when (seq (figwheel.repl/connections-available env))
        (cljs.repl/-evaluate env
                             "<cljs repl>"
                             1
                             "tutorial.moan.frontend.main.re_fetch()")))))


(defmethod ig/init-key ::frontend
  [_ _]
  (frontend))
