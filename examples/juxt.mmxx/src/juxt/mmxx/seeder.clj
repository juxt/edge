;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.seeder
  (:require
   [integrant.core :as ig]
   [crux.api :as crux]))

(defmethod ig/init-key ::seeder [_ {:keys [crux]}]
  (crux/submit-tx
   crux
   [[:crux.tx/put
     {:crux.db/id (java.net.URI. "/")
      :content "Hello World!"}]]))
