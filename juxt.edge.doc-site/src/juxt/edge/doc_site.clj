(ns juxt.edge.doc-site
  (:require
    [integrant.core :as ig]
    [clojure.java.io :as io]
    [edge.asciidoctor :refer [load-doc]]
    [yada.yada :as yada]))

(defn routes [engine]
  [["" (merge
         (yada/redirect ::doc-resource {:route-params {:name "index"}})
         {:id ::doc-index})]
   [[;; regex derived from bidi's default, but adding / to allow directories
     [#"[A-Za-z0-9\\-\\_\\.\/]+" :name] ".html"]
    (yada/resource
      {:id ::doc-resource
       :methods
       {:get
        {:produces [{:media-type "text/html;q=0.8" :charset "utf-8"}
                    {:media-type "application/json"}]
         :response (fn [ctx]
                     (let [path (str "doc/sources/" (-> ctx :parameters :path :name) ".adoc")]
                       (try
                         (.convert
                           (load-doc
                             ctx
                             engine
                             (-> ctx :parameters :path :name)
                             (slurp (io/resource path))))
                         (catch Exception e
                           (throw (ex-info (format "Failed to convert %s" path)
                                           {:path path} e))))))}}})]])

(defmethod ig/init-key ::routes [_ {:keys [edge.asciidoctor/engine]}]
  (routes engine))
