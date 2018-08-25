;; Copyright © 2016-2018, JUXT LTD.

(ns edge.asciidoctor
  (:require
   [aero.core :as aero]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.repl :refer [source source-fn]]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [hiccup.core :as h]
   [integrant.core :as ig]
   [yada.yada :as yada]))

(defn- coerce-from-ruby-decorator
  "Attributes are given to Asciidoctorj macro callbacks as instances of
  the org.asciidoctor.internal.RubyAttributesMapDecorator class, which
  doesn't behave as a Clojure map. This function coerces to a Clojure
  map."
  [attrs]
  (into {} attrs))

(defn- document-attributes
  "Return the document's attributes from a Asciidoctorj node."
  [el]
  (.getAttributes (.getDocument el)))

(defn engine []
  (let [engine (org.asciidoctor.Asciidoctor$Factory/create)
        reg (.javaExtensionRegistry engine)]

    (.docinfoProcessor
      reg
      (proxy [org.asciidoctor.extension.DocinfoProcessor] []
        (process [document]
          (slurp (io/resource "doc/docinfo.html")))))

    (.postprocessor
      reg
      (proxy [org.asciidoctor.extension.Postprocessor] []
        (process [document output]
          (string/replace
            output "</html>"
            (str (slurp (io/resource "doc/docinfo-footer.html"))
                 "</html>")))))

    (.includeProcessor
      reg
      (proxy [org.asciidoctor.extension.IncludeProcessor] []
        (handles [target] true)
        (process [parent reader target attributes]
          (condp re-matches target
            #"config:(.*):(.*)"
            :>> (fn [[_ k path]]
                  (.push_include
                    reader
                    (-> (io/resource "config.edn")
                        (aero/read-config {:profile (keyword k)})
                        (get (keyword path))
                        pr-str)
                    target target 1 attributes))

            #"src:(.*)"
            :>> (fn [[_ s]]
                  (.push_include
                    reader
                    (or (source-fn (symbol s)) "source not found")
                    target target 1 attributes))

            #"srcblk:(.*)"
            :>> (fn [[_ s]]
                  (.push_include
                    reader
                    (format
                      "[source,clojure]\n%s\n----\n%s\n----\n"
                      (if (contains? (set (vals (into {} attributes))) "title")
                        (let [{:keys [file line]} (meta (resolve (symbol s)))]
                          (format ".link:/sources/src/%s[%s] at line %s" file file line))
                        "")
                      (or (source-fn (symbol s)) "source not found"))
                    target target 1 attributes))

            #"resource:(.*)"
            :>> (fn [[_ path]]
                  (.push_include reader (if-let [res (io/resource path)]
                                          (slurp res)
                                          "resource not found")
                                 target target 1 attributes))

            ;; Else
            (if-let [f (io/resource (str "doc/" target))]
              (.push_include reader (slurp f) target target 1 attributes)
              (log/info "Failed to find" target))))))

    (.inlineMacro
      reg
      (proxy [org.asciidoctor.extension.InlineMacroProcessor] ["srcloc"]
        (getRegexp []
          (log/infof "Calling getRegexp!")
          )
        (process [parent target attributes]
          (let [{:keys [file line]} (some-> target symbol resolve meta)]
            (format "%s %s of link:/sources/src/%s[%s]"
                    (if
                        (contains? (set (vals (into {} attributes))) "titlecase")
                      "Line" "line")
                    line file file)))))

    engine))

(defn load-doc [ctx engine docname content]
  (assert engine)
  (.load
    engine
    content
    (java.util.HashMap. ; Asciidoctor is in JRuby which takes HashMaps
      {"safe" org.asciidoctor.SafeMode/UNSAFE
       "header_footer" true
       "to_file" false
       "backend" "html5"
       "attributes"
       (java.util.HashMap.
         (merge
           (edn/read-string
             (slurp
               (io/file "resources/asciidoctor/attributes.edn")))
           {"docname" docname}))})))

(defn documentation-routes [config]
  (let [asciidoc-engine (:edge/asciidoctor config)]
    (assert asciidoc-engine)
    ["/"
     [
      ["" (yada/redirect ::doc-resource {:route-params {:name "index"}})]
      ["doc"
       [
        [#{"" "/"} (yada/redirect ::doc-resource {:route-params {:name "index"}})]
        [["/" :name ".html"]
         (yada/resource
           {:id ::doc-resource
            :methods
            {:get
             {:produces [{:media-type "text/html;q=0.8" :charset "utf-8"}
                         {:media-type "application/json"}]
              :response (fn [ctx]
                          (let [path (str "doc/" (-> ctx :parameters :path :name) ".adoc")]
                            (.convert
                              (load-doc
                                ctx
                                asciidoc-engine
                                (-> ctx :parameters :path :name)
                                (slurp (io/resource path))))))}}})]]]]]))

(defmethod ig/init-key :edge/asciidoctor [_ config]
  (engine))
