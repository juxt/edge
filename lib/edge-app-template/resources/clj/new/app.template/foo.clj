(ns {{root-ns}}.foo
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
  (println "Creating state atom in {{root-ns}}.foo (src/{{sanitized}}/foo.clj)")
  (atom init)){{/web}}
