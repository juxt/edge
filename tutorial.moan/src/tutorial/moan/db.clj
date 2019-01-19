(ns ^{:clojure.tools.namespace.repl/load false} tutorial.moan.db
  "A mock database which will simulate storage of EDN data."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io])
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
            {:users {"sparks0id" {:name "Malcolm Sparks"}
                     "m0nr03" {:name "Dominic Monroe"
                               :follows ["sparks0id"]}}
             :moans [{:author "m0nr03"
                      :text "Vim rules! jjjjjjjjjk<ESC><Enter>"}
                     {:author "sparks0id"
                      :text "Vim is not a lisp editor!!!!1"}]}))))

(locking file-lock
  (when-not (.exists file)
    (reset)))

(defn store
  [x]
  (locking file-lock
    (spit file (->edn x))))

(defn read
  []
  (locking file-lock
    (edn/read (-> file io/reader clojure.lang.LineNumberingPushbackReader.))))
