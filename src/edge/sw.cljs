(ns edge.sw
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [chan put! <!]]
            [edge.net]))

(def app-state (r/atom {}))

(defn get-sw-data []
  (println "Loading starwars data")

  (let [c (edge.net/sse-chan "/starwars/xwing-messages")]
    (go
      (loop []
        (when-let [v (<! c)]
          (swap! app-state update-in [:xwing] conj v)
          (println v)
          (recur)))))

  (doto
      (new js/XMLHttpRequest)
      (.open "GET" "/starwars/people")
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
       [:h1 "Xwing coordination messages"]
       [:table
        (for [msg (:xwing state)]
          [:tr
           [:td msg]])]
       [:h1 "Starwars people"]
       (for [{:keys [name]} (sort-by :name (:results (:sw state)))]
         [:p (if (re-find #"Luke" name) [:b name] name)])])))

(defn init [section]
  (get-sw-data)
  (r/render-component [sw] section))
