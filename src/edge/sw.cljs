(ns edge.sw
  (:require [reagent.core :as r]
            [cljs.reader :refer [read-string]]))

(def app-state (r/atom {}))

(defn get-sw-data []
  (println "Loading starwars data")
  (doto
      (new js/XMLHttpRequest)
      (.open "GET" "/starwars")
      (.setRequestHeader "Accept" "application/edn")
      (.addEventListener
       "load"
       (fn [evt]
         (swap! app-state
                assoc :sw
                (read-string evt.currentTarget.responseText))))
      (.send)))

(defn sw []
  (fn []
    (let [state @app-state]
      [:div
       [:p "In a galaxy far far away"]
;;       [:p (prn-str (keys (:sw  state)))]
       (for [{:keys [name]} (sort-by :name (:results (:sw state)))]
         [:p (if (re-find #"Luke" name) [:b name] name)])])))

(defn init [section]
  (get-sw-data)
  (r/render-component [sw] section))
