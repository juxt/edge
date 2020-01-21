(ns juxt.calcite-play.sw-load
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]))

(defn- keywordize-fields
  [fields]
  (reduce-kv (fn [acc k v]  (conj acc [(keyword k) v])) {} fields))

                                        ; TODO: Make these URNs

(defn ->crux-doc [json]
    (let [{:strs [fields model pk]} json
          id (keyword model (str "id" pk))
          ]
      (-> fields
          keywordize-fields
          (conj {:crux.db/id id}))))

(defn res->crux-docs [res]
  (map ->crux-doc (json/parse-stream (io/reader res))))
