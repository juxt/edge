(ns ^{:clojure.tools.namespace.repl/load false}
  load-krei
  (:require
    [io.dominic.krei.alpha.core :as krei]))

(def krei (krei/watch))
