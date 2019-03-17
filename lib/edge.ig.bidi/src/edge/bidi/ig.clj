(ns edge.bidi.ig
  (:require
    bidi.vhosts
    [integrant.core :as ig]))

(defmethod ig/init-key ::vhost
  [_ args]
  (apply bidi.vhosts/vhosts-model args))
