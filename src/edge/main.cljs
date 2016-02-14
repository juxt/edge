;; Copyright © 2016, JUXT LTD.

(ns edge.main
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cognitect.transit :as t]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]
   [goog.dom :as gdom]
   [cljs.core.async :as a :refer [chan >! <! close! timeout alts! mult tap mix pub sub]]))

(defn init []
  (enable-console-print!)
  (println "Congratulations - your environment seems to be working"))
