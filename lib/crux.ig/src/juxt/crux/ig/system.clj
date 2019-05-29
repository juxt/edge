(ns juxt.crux.ig.system
  (:require [clojure.java.io :as io]
            crux.api
            [integrant.core :as ig])
  (:import java.nio.file.attribute.FileAttribute
           java.nio.file.Files))

(defn- delete-directory
  "Delete a directory and all files within"
  [f]
  (run! io/delete-file (filter #(.isFile %) (file-seq f)))
  (run! io/delete-file (reverse (file-seq f))))

(def ^:private tmp-dir
  ;; tools refresh wipes this out, to which I don't have an easy solution.
  ;; I could move it to another namespace which has reloading disabled perhaps.
  (memoize
   (fn [identity]
     (let [path (Files/createTempDirectory nil (into-array FileAttribute []))]
       (.addShutdownHook
         (Runtime/getRuntime)
         (new Thread (fn [] (delete-directory (.toFile path)))))
       (.mkdir (.toFile path))
       (str path)))))

(defmethod ig/halt-key! :juxt.crux.ig/system
  [_ system]
  (.close system))

(defmethod ig/prep-key :juxt.crux.ig/system
  [k opts]
  (cond-> opts
    (not (contains? opts :event-log-dir))
    (assoc :event-log-dir (tmp-dir [k "event-log"]))
    (not (contains? opts :db-dir))
    (assoc :db-dir (tmp-dir [k "db-dir"]))))

(derive ::standalone :juxt.crux.ig/system)

(defmethod ig/init-key ::standalone
  [_ opts]
  (crux.api/start-standalone-system opts))

(derive ::cluster-node :juxt.crux.ig/system)

(defmethod ig/init-key ::cluster-node
  [_ opts]
  (crux.api/start-cluster-node opts))
