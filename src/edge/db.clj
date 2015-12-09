(ns edge.db
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :refer [infof errorf]]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   clojure.tools.reader
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :refer [closed? Channel]]
   [com.stuartsierra.component :as component]
   [datomic.api :as d]
   [schema.core :as s]))

(s/defrecord DbUriGenerator [uri :- String]
  component/Lifecycle
  (start [component]
    (assoc component :uri (format "datomic:sql://edge/test/%s?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic" (d/squuid))))
  (stop [component] component))

(defn new-dburi-generator []
  (map->DbUriGenerator {}))

(defn apply-schema [conn]
  (if-let [res (io/resource "schema.edn")]
    (with-open [rdr (java.io.PushbackReader. (io/reader res))]
      (do
        @(d/transact conn
                     (binding [clojure.tools.reader/*data-readers*
                               {'db/id datomic.db/id-literal
                                'db/fn datomic.function/construct
                                'base64 datomic.codec/base-64-literal}]
                       (clojure.tools.reader/read (indexing-push-back-reader rdr))))
        (infof "Created schema")))
    (errorf "No schema found")))

(s/defrecord Database [dburi :- DbUriGenerator]
  component/Lifecycle
  (start [component]
    (let [uri (:uri dburi)]
      (infof "Creating database: %s" uri)
      (let [cdres (d/create-database uri)]
        (infof "Created database result is %s" cdres))
      (let [conn (d/connect uri)]
        (infof "Conn is %s" conn)
        (apply-schema conn)))
    component)
  (stop [component]
    (d/delete-database (:uri dburi))
    (d/shutdown false)
    component))

(defn new-database []
  (component/using (map->Database {}) [:dburi]))

(defn add-messages [conn]
  (doseq [msg ["Hi!" "Are you there?" "This is my last message"]]
    @(d/transact
      conn
      [[:db/add (d/tempid :db.part/user) :message msg]])))

(s/defrecord Seeder [dburi :- DbUriGenerator
                     ch :- (s/protocol Channel)
                     database :- Database]
  component/Lifecycle
  (start [component]
    (let [uri (:uri dburi)]
      (infof "Creating seeder")
      (let [conn (d/connect uri)]
        (let [ch (a/chan)]
          (a/thread
            (loop []
              (when-not (closed? ch)
                (Thread/sleep 4000)
                (add-messages conn)
                (recur))))
          (assoc component :ch ch)))))
  (stop [component]
    (when ch (a/close! ch))
    component))

(defn new-seeder []
  (component/using
   (map->Seeder {})
   [:database :dburi]))

