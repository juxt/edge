(ns clj.new.edge-app-template
  (:require [clj.new.templates :refer [renderer project-name name-to-path ->files
                                       multi-segment sanitize-ns
                                       *force?* *dir*]]))

(def render (renderer "app.template"))

(defn- generate-port
  "Generates 'pretty' port sequences in the form of:
  xx00
  xyxy"
  []
  (let [base 20
        upper-limit 99
        dd-port (+ (rand-int (- (inc upper-limit) base)) base)]
    (rand-nth
      [(format "%d%d" dd-port dd-port)
       (format "%d00" dd-port)])))


(def supported-frameworks ["sass" "cljs" "reframe"])

(def framework-opts (set (map #(str "--" %) supported-frameworks)))

(def supported-attributes #{"cljs"})

(def attribute-opts (set (map #(str "--" %) supported-attributes)))

(defn- next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                          diagonal
                          (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn- levenshtein
  [sequence1 sequence2]
  (peek
    (reduce (fn [previous current] (next-row previous current sequence2))
            (map #(identity %2) (cons nil sequence2) (range))
            sequence1)))

(defn- similar [ky ky2]
  (let [dist (levenshtein (str ky) (str ky2))]
    (when (<= dist 2) dist)))

(defn similar-options [opt]
  (second (first (sort-by first
                  (filter first (map (juxt (partial similar opt) identity)
                                     (concat framework-opts attribute-opts)))))))

(defn parse-opts [opts]
  (reduce (fn [accum opt]
            (cond
              (or
               (framework-opts opt)
               (attribute-opts opt)) (conj accum (keyword (subs opt 2)))
              :else
              (let [suggestion (similar-options opt)]
                (try
                  (throw
                   (ex-info
                    (format "Unknown option '%s' %s"
                            opt
                            (str
                             (when suggestion
                               (format "\n    --> Perhaps you intended to use the '%s' option?" suggestion))))
                    {:opts opts}))
                  (catch Exception e
                    (println (.toString e)))
                  (finally
                    (System/exit 0))))))
          #{} opts))

(defn edge-app-template
  "FIXME: write documentation"
  [name & opts]
  (binding [*dir* (sanitize-ns name)]
    (let [parsed-opts (parse-opts opts)
          cljs? (contains? parsed-opts :cljs)
          sass? (contains? parsed-opts :sass)
          reframe? (contains? parsed-opts :reframe)
          data {:name (project-name name)
                :sanitized (name-to-path name)
                :root-ns (multi-segment (sanitize-ns name))
                :sass sass?
                :cljs (or cljs? reframe?)
                :reframe reframe?
                :kick (or sass?
                          reframe?
                          cljs?)
                :server-port (generate-port)
                :figwheel-port (generate-port)}]
      (println (str "Generating fresh 'clj new' edge.app-template project into " *dir* "."))
      (->files data
               ["deps.edn" (render "deps.edn" data)]
               ["src/{{sanitized}}/foo.clj" (render "foo.clj" data)]
               ["src/config.edn" (render "config.edn" data)]
               ["dev/dev.clj" (render "dev.clj" data)]
               ["dev/log_dev_app.properties" (render "log_dev_app.properties" data)]
               [".dir-locals.el" (render "dir-locals.el" data)])
      (binding [*force?* true]
        (when (:kick data)
          (->files data
                   ["src/index.html" (render "index.html" data)]
                   ["target/dev/.gitkeep" ""]
                   ["target/prod/.gitkeep" ""]))
        (if (:sass data)
          (->files data
                   ["src/{{name}}.scss" (render "app.css" data)])
          (->files data
                   ["src/public/{{name}}.css" (render "app.css" data)]))
        (when (:cljs data)
          (->files data
                   ["src/{{sanitized}}/frontend/main.cljs"
                    (render (cond
                              reframe? "reframe/main.cljs"
                              cljs? "main.cljs")
                            data)]))
        (when (:reframe data)
          (->files data
                   ["src/{{sanitized}}/frontend/views.cljs" (render "reframe/views.cljs" data)]
                   ["src/{{sanitized}}/frontend/db.cljs" (render "reframe/db.cljs" data)]
                   ["src/{{sanitized}}/frontend/handlers.cljs" (render "reframe/handlers.cljs" data)]
                   ["src/{{sanitized}}/frontend/subs.cljs" (render "reframe/subs.cljs" data)]))))))
