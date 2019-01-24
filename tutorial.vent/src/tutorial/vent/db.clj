(ns ^{:clojure.tools.namespace.repl/load false} tutorial.vent.db
  "A mock database which will simulate storage of EDN data."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [tutorial.vent.reload :as vent.reload])
  (:refer-clojure :exclude [read]))

(def ^:private file-lock (Object.))
(def ^:private file (io/file "db.edn"))

(defn- ->edn
  [x]
  (binding [*print-length* nil
            *print-level* nil
            *print-namespace-maps* false]
    (let [edn-str (prn-str x)]
      ;; Confirm that the edn reads back, i.e. there are no #object or similar
      ;; that can't be read
      (try
        (edn/read-string edn-str)
        (catch Exception e
          (throw (ex-info "Your data cannot be read back safely" {:data x}
                          e))))
      edn-str)))

(defn reset
  []
  (locking file-lock
    (spit file
          (->edn
            {:users {"jane_smith" {:name "Jane Smith"}
                     "john_smith" {:name "John Smith"
                                   :follows ["works_for_pizza"]}
                     "works_for_pizza" {:name "Edit Me!"
                                        :follows ["john_smith"]}}
             :vents [{:username "jane_smith"
                      :text "A tweet from the database"}
                     {:username "john_smith"
                      :text "Another tweet from the database"
                      :favorite? true}]}))
    (vent.reload/frontend)))

(locking file-lock
  (when-not (.exists file)
    (reset)))

(defn store
  [x]
  (locking file-lock
    (spit file (->edn x))
    (vent.reload/frontend)))

(defn read
  []
  (locking file-lock
    (edn/read (-> file io/reader clojure.lang.LineNumberingPushbackReader.))))
