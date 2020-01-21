(ns juxt.calcite-play.sw-load
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]))

(defn- keywordize-fields
  [fields]
  (reduce-kv (fn [acc k v]  (conj acc [(keyword k) v])) {} fields))

(def model->path
  {"resources.planet" "planets"})

(defn ->crux-doc [json]
    (let [{:strs [fields model pk]} json
          id (java.net.URI. (format "https://swapi.co/api/%s/%s/" (model->path model) pk))]
      (-> fields
          keywordize-fields
          (conj {:crux.db/id id}))))

(defn res->crux-docs [res]
  (map ->crux-doc (json/parse-stream (io/reader res))))

(comment
  (first (json/parse-stream (io/reader (io/resource "swapi/resources/fixtures/planets.json")))))

(comment
  (res->crux-docs (io/resource "swapi/resources/fixtures/planets.json")))
