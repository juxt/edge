(ns ^:figwheel-hooks gig.swars.frontend.main
  (:require [reagent.core :as r]
            [gig.swars.frontend.components :as c]
            [gig.swars.frontend.api :as api]))

(defn app []
  [c/page])

(defn render []
  (r/render
   [app]
   (.getElementById js/document "app")))

;; This is called once
(defonce init
  (render))

;; This is called every time you make a code change
(defn ^:after-load reload []
  (render))
