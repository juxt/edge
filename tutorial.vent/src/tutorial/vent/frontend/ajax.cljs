(ns tutorial.vent.frontend.ajax
  (:require
    [cljs.reader :refer [read-string]]))

(defmulti page-fetch :name)

(defmethod page-fetch :default
  [_]
  nil)

(defn fetch-page
  [page state*]
  (some-> (page-fetch page)
          (.then (fn [data] (.text data)))
          (.then (fn [text] (read-string text)))
          (.then (fn [x] (swap! state*
                                (fn [state]
                                  (if (= page (:page state))
                                    (assoc-in state [:page :data] x)
                                    state)))))))
