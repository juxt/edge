(ns clj.new.edge-app-template
  (:require [clj.new.templates :refer [renderer project-name name-to-path ->files
                                       multi-segment sanitize-ns]]))

(def render (renderer "app.template"))

(defn edge-app-template
  "FIXME: write documentation"
  [name]
  (let [data {:name (project-name name)
              :sanitized (name-to-path name)
              :root-ns (multi-segment (sanitize-ns name))}]
    (println "Generating fresh 'clj new' edge.app-template project.")
    (->files data
             ["deps.edn" (render "deps.edn" data)]
             ["src/{{sanitized}}/foo.clj" (render "foo.clj" data)]
             ["src/config.edn" (render "config.edn" data)]
             ["dev/dev.clj" (render "dev.clj" data)])))
