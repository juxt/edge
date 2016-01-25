 (ns edge.main
   (:require [om.next :as om :refer-macros [defui]]
             [om.dom :as dom]
             [goog.dom :as gdom]))

(defn init []
  (enable-console-print!)
  (println "Congratulations - your environment seems to be working"))

