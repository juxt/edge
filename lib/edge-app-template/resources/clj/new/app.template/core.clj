(ns {{root-ns}}.core
{{#web}}  (:require
    [yada.yada :as yada]
    [integrant.core :as ig]))

(defn string-resource
  [x]
  (yada/as-resource x))

(defmethod ig/init-key ::string
  [_ x]
  (string-resource x)){{/web}}{{^web}}  (:require
    [integrant.core :as ig]))

(defmethod ig/init-key ::state
  [_ init]
  (println "Creating state atom in {{root-ns}}.core (src/{{sanitized}}/core.clj)")
  (atom init)){{/web}}
