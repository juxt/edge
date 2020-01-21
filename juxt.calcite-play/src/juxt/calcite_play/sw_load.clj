(ns juxt.calcite-play.sw-load
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]))

(defn ->ref [v k]
  (java.net.URI. (format "https://swapi.co/api/%s/%s/"
                         (case k
                           "characters" "people"
                           "homeworld" "planets"
                           "pilots" "people"
                           k)
                         v)))

(defn ->ref-or-refs [v k]
  (if (sequential? v)
    (vec (map #(->ref % k) v))
    (->ref v k)))

(defn- map-fields
  [fields]
  (reduce-kv
   (fn [acc k v]
     (conj acc [(keyword k)
                (cond-> v
                  (#{"people" "planets" "films" "species"
                     "vehicles" "starships" "characters"
                     "homeworld" "pilots"} k)
                  (->ref-or-refs k)
                  )])) {} fields))

;; All the resource paths making up Star Wars DB
(def resource-paths
  ["swapi/resources/fixtures/people.json"
   "swapi/resources/fixtures/planets.json"
   "swapi/resources/fixtures/films.json"
   "swapi/resources/fixtures/species.json"
   "swapi/resources/fixtures/vehicles.json"
   "swapi/resources/fixtures/starships.json"])

;; Primary keys are strings of the form resources.<name> which we need to map to
;; paths in the canonical URI
(def model->path
  {"resources.film" "films"
   "resources.people" "people"
   "resources.planet" "planets"
   "resources.species" "species"
   "resources.starship" "starships"
   "resources.vehicle" "vehicles"})

(defn ->crux-doc [json]
  (let [{:strs [fields model pk]} json
        path (model->path model)
        _ (assert path (format "nil path with model %s" model))
        id (java.net.URI. (format "https://swapi.co/api/%s/%s/" path pk))]
    (-> fields
        map-fields
        (conj {:crux.db/id id}))))

(defn res->crux-docs [res]
  (map ->crux-doc (json/parse-stream (io/reader res))))

(comment
  (take 4 (json/parse-stream (io/reader (io/resource "swapi/resources/fixtures/vehicles.json")))))

(comment
  (for [res resource-paths]
    (res->crux-docs (io/resource res))))

(comment
  (res->crux-docs (io/resource "swapi/resources/fixtures/vehicles.json")))
