(ns edge.kick
  (:require
   [integrant.core :as ig]
   [juxt.kick.alpha.core :as kick]
   ;; Load built-in providers' defmethods
   ;;juxt.kick.alpha.providers.shadow-cljs
   juxt.kick.alpha.providers.figwheel
   juxt.kick.alpha.providers.sass
   [edge.system :as system]))

(comment
  (clojure.java.io/resource "edge/phonebook_app.cljs")
  (kick/mybuild (:edge.kick/builder (edge.system/system-config {:profile :prod}))))

(defmethod ig/init-key :edge.kick/builder
  [_ v]
  (kick/watch v))

(defmethod ig/halt-key! :edge.kick/builder
  [_ close]
  (close))

(defn- clean
  [path]
  (letfn [(removable-files [f]
            (remove #(= (.getName %) ".gitkeep") (.listFiles f)))
          (clean [f]
            (when (.isDirectory f)
              (doseq [f (removable-files f)]
                (clean f)))
            (.delete f))]
    (doseq [path (removable-files path)]
      (clean path))))

(defn -main
  [& [target-arg]]
  (let [kick-init-config (:edge.kick/builder (system/system-config {:profile :prod}))
        kick-config (assoc kick-init-config
                           :kick.builder/target
                           (or target-arg
                               (:kick.builder/target kick-init-config)))]
    ;; When a target-arg is given, assume that we are not reusing a dir.
    ;; Otherwise assume the directory is being reused and we are the first
    ;; thing being run.
    (when-not target-arg
      (clean (java.io.File. (:kick.builder/target kick-config))))
    (kick/build-once kick-config)))
