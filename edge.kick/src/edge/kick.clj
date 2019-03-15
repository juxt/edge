(ns edge.kick
  (:require
    [edge.system :as system]
    [edge.kick.builder :refer [load-provider-namespaces]]
    [juxt.kick.alpha.core :as kick]))

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
  (let [config (system/config {:profile :prod})
        kick-init-config (get-in config [:edge.kick/config]
                                 (get-in config [:ig/system :edge.kick/builder]))
        kick-config (assoc kick-init-config
                           :kick.builder/target
                           (or target-arg
                               (:kick.builder/target kick-init-config)))]
    (load-provider-namespaces kick-config)
    ;; When a target-arg is given, assume that we are not reusing a dir.
    ;; Otherwise assume the directory is being reused and we are the first
    ;; thing being run.
    (when-not target-arg
      (clean (java.io.File. (:kick.builder/target kick-config))))
    (kick/build-once kick-config)))
