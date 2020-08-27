(ns dev
  (:require
   [dev-extras :refer :all]
   [crux.api :as crux]))

;; Add your helpers here
(defn crux-node []
  (:juxt.crux.ig/system system))

(defn db []
  (crux/db (crux-node)))

(defn e [id]
  (crux/entity (db) id))

(defn q [query]
  (crux/q (db) query))
