(ns ^:figwheel-hooks {{root-ns}}.frontend.main
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [{{name}}.views :as views]
            [{{name}}.handlers :as handlers] ))

(defn mount-root []
  (when-let [section (.getElementById js/document "app")]
    (rf/clear-subscription-cache!)
    (r/render [views/main-panel] section)))

(defn init []
  (rf/dispatch-sync [::handlers/initialize-db])
  (enable-console-print!)
  (mount-root))

;; This is called every time you make a code change
(defn ^:after-load reload []
  (mount-root))

(defonce run (init))
