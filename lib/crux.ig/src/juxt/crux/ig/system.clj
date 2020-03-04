(ns juxt.crux.ig.system
  (:require [clojure.java.io :as io]
            [crux.api :as api]
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
   (fn [_]
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
    (not (contains? opts :crux.standalone/event-log-dir))
    (assoc :crux.standalone/event-log-dir (tmp-dir [k "event-log"]))
    (not (contains? opts :crux.kv/db-dir))
    (assoc :crux.kv/db-dir (tmp-dir [k "db-dir"]))))

(defmethod ig/init-key :juxt.crux.ig/system
  [_ opts]
  (api/start-node opts))

(derive ::standalone :juxt.crux.ig/system)

;; Crux has deprecated start-standalone-node. start-node should be used
;; instead. As such this modules abstaction of standalone and cluster nodes is
;; no longer relevant and are deprecated.
;; :juxt.crux.ig/system should be used instead, with the relevant topologies
;; supplied.
(defmethod ig/init-key ::standalone
  [_ opts]
  (api/start-node
    (update opts :crux.node/toplogy
            (fn [seq] (vec (cons 'crux.standalone/topology seq))))))

(derive ::cluster-node :juxt.crux.ig/system)

;; If using this modules the juxt/crux-kafka dependancy should be included.
(defmethod ig/init-key ::cluster-node
  [_ opts]
  (api/start-node
    (update opts :crux.node/toplogy
            (fn [seq] (vec (cons 'crux.kafka/topology seq))))))
