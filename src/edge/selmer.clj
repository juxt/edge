;; Copyright © 2016, JUXT LTD.

(ns edge.selmer
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [selmer.filter-parser :refer [compile-filter-body]]
   [schema.core :as s]
   [hiccup.core :refer [html]]
   [selmer.parser :as selmer]
   [yada.yada :as yada]))

(defn- make-url-fn [k]
  (fn [args context-map]
    (when-let [ctx (:ctx context-map)]
      (get (yada/uri-info ctx
                          (keyword "edge.resources" (first args))
                          {:route-params
                           (reduce (fn [acc [k v]]
                                     (assoc acc
                                            (keyword k)
                                            ((compile-filter-body v) context-map)))
                                   {}
                                   (partition 2 (rest args)))})
           k))))

(defn add-url-tag!
  "Add a tag that gives access to yada's uri-info function in templates"
  []
  (selmer/add-tag! :href (make-url-fn :href))
  (selmer/add-tag! :url (make-url-fn :uri))
  (selmer/add-tag! :source (fn [args context-map]
                             (html [:tt [:a {:href (str "/sources/" (first args))} (first args)]]))))

(s/defrecord Selmer [template-caching? :- s/Bool]
  Lifecycle
  (start [component]
    (selmer/set-resource-path! (io/resource "templates"))

    (if template-caching?
      (selmer.parser/cache-on!)
      (selmer.parser/cache-off!))

    (add-url-tag!))

  (stop [component]
    component))

(defn new-selmer []
  (map->Selmer {}))
