(ns dev
  (:require
   [bidi.bidi :as bidi]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer [run-all-tests]]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [edge.system :refer (new-system-map new-dependency-map)]
   [schema.core :as s]))

(def system nil)

(defn check
  "Check for component validation errors"
  []
  (let [errors
        (->> system
             (reduce-kv
              (fn [acc k v]
                (assoc acc k (s/check (type v) v)))
              {})
             (filter (comp some? second)))]

    (when (seq errors) (into {} errors))))

(defn new-dev-system
  "Create a development system"
  []
  (-> (new-system-map {})
      (component/system-using (new-dependency-map))))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (new-dev-system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system component/start)
  (when-let [errors (check)] (println "Warning, component integrity violated!" errors)))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))



(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  :ok
  )

(defn reset []
  (stop)
  (refresh :after 'dev/go))

(defn test-all []
  (run-all-tests #"edge.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

;; REPL Convenience helpers

(defn change-greeting [greeting]
  (swap! (get-in system [:edge.system/data :model]) assoc :greeting greeting))
