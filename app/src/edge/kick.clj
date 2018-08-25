(ns edge.kick
  (:require
   [integrant.core :as ig]
   [juxt.kick.alpha.core :as kick]
   ;; Load built-in providers' defmethods
   ;;juxt.kick.alpha.providers.shadow-cljs
   juxt.kick.alpha.providers.figwheel
   juxt.kick.alpha.providers.sass))

(defmethod ig/init-key :edge.kick/builder
  [_ v]
  (kick/watch v))

(defmethod ig/halt-key! :edge.kick/builder
  [_ close]
  (close))
