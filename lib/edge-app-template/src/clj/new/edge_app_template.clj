(ns clj.new.edge-app-template
  (:require
    [clj.new.templates :refer [renderer project-name name-to-path ->files
                               multi-segment sanitize-ns
                               *force?* *dir*]]
    [clojure.java.io :as io])
  (:import
    [java.nio.file Files Paths]
    [java.nio.file.attribute FileAttribute]))

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


(def supported-flags
  {"sass" {:description "Configure a sass builder for this app"}
   "cljs" {:description "Configure figwheel and cljs for this app"}
   "reframe" {:description "Configure skeleton reframe structure, implies --cljs"}
   "help" {:description "Show help"}})

(def flags-opts (set (map #(str "--" %) (conj (keys supported-flags) "no-web"))))

;;The following functions involving the flags validation are taken from:
;;https://github.com/bhauman/figwheel-main-template/blob/master/src/leiningen/new/figwheel_main.clj

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
                                             flags-opts))))))
(defn parse-opts [opts]
  (reduce (fn [accum opt]
            (cond
              (flags-opts opt) (conj accum (keyword (subs opt 2)))
              :else
              (let [suggestion (similar-options opt)]
                (reduced
                 {:error
                  (format
                   "Unknown option '%s' %s" opt
                   (str
                    (when suggestion
                      (format
                       "\n --> Perhaps you intended to use the '%s' option?" suggestion))))}))))
          #{} opts))

(defn usage
  []
  (println "Usage:")
  (println "  bin/app GROUP/PROJECT [OPTION]...")
  (println)
  (println "Options:")
  (doseq [[flag {:keys [description]}] supported-flags]
    (println (str "  --" flag "\t" description)))
  (println)
  (println "Examples:")
  (doseq [example ["acme/api" "acme/dashboard --cljs" "acme/radar --sass --cljs"]]
    (println "  bin/app" example)))

(defn symlink
  [link-name destination]
  (let [dest-path (Paths/get *dir* (into-array [destination]))]
    (io/make-parents (.toFile dest-path))
    (Files/createSymbolicLink
      dest-path
      (.relativize (.getParent dest-path)
                   (Paths/get *dir*
                              (into-array
                                ["../lib/edge-app-template/links/"
                                 link-name])))
      (into-array FileAttribute []))))

(defn edge-app-template
  "FIXME: write documentation"
  [name & opts]
  (binding [*dir* (sanitize-ns name)]
    (let [parsed-opts (parse-opts opts)]
      (cond
        (:error parsed-opts)
        (do (println (:error parsed-opts))
            (System/exit 1))

        (or (contains? parsed-opts :help)
            ;; Passed by bin/app when no arguments are provided.
            (= name "edge/internal.help"))
        (do (usage)
            (System/exit 0))

        :else
        (let [cljs? (contains? parsed-opts :cljs)
              sass? (contains? parsed-opts :sass)
              reframe? (contains? parsed-opts :reframe)
              web? (not (contains? parsed-opts :no-web))
              data {:name (project-name name)
                    :sanitized (name-to-path name)
                    :root-ns (multi-segment (sanitize-ns name))
                    :web web?
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
                   ["src/{{sanitized}}/core.clj" (render "core.clj" data)]
                   ["src/config.edn" (render "config.edn" data)]
                   ["dev/dev.clj" (render "dev.clj" data)]
                   ["dev/log_dev_app.properties" (render "log_dev_app.properties" data)]
                   [".dir-locals.el" (render "dir-locals.el" data)]
                   (when (:kick data) ["src/index.html" (render "index.html" data)])
                   (when (:kick data) ["target/dev/.gitkeep" ""])
                   (when (:kick data) ["target/prod/.gitkeep" ""])
                   (when web?
                     (if (:sass data)
                       ["src/{{name}}.scss" (render "app.css" data)]
                       ["src/public/{{name}}.css" (render "app.css" data)]))

                   (when (:cljs data)
                     ["src/{{sanitized}}/frontend/main.cljs"
                      (render (cond
                                reframe? "reframe/main.cljs"
                                cljs? "main.cljs")
                              data)])

                   (when (:reframe data)
                     ["src/{{sanitized}}/frontend/views.cljs" (render "reframe/views.cljs" data)])
                   (when (:reframe data)
                     ["src/{{sanitized}}/frontend/db.cljs" (render "reframe/db.cljs" data)])
                   (when (:reframe data)
                     ["src/{{sanitized}}/frontend/handlers.cljs" (render "reframe/handlers.cljs" data)])
                   (when (:reframe data)
                     ["src/{{sanitized}}/frontend/subs.cljs" (render "reframe/subs.cljs" data)]))
          (when (:cljs data)
            (symlink "cljs_calva_settings.json" ".vscode/settings.json")))))))
