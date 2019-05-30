(ns juxt.crux-ui.frontend.views.facade
  (:require [juxt.crux-ui.frontend.views.query-ui :as q]))


(defn root []
  [:div#root.root
   [q/query-ui]])
