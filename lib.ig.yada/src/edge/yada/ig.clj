(ns edge.yada.ig
  (:require
    [yada.yada :as yada]
    [integrant.core :as ig]))

(defmethod ig/init-key ::listener
  [_ opts]
  (apply yada/listener opts))

(defmethod ig/halt-key! ::listener
  [_ {:keys [close]}]
  (when close (close)))
