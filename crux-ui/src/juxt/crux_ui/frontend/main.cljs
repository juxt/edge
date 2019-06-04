(ns juxt.crux-ui.frontend.main
  (:require [reagent.core :as r]
            [cljss.core :as css]
            [re-frame.core :as rf]
            [juxt.crux-ui.frontend.views.facade :as views]
            [juxt.crux-ui.frontend.events.facade :as events]))


(def example-query-str
  (with-out-str
    (cljs.pprint/pprint
      '{:full-results? true
        :find [e]
        :where [[e :name "Pablo"]]})))

(def default-db
  {:db.query/input  example-query-str
   :db.query/error  nil
   :db.query/result nil})


(defn mount-root []
  (r/render [views/root] (js/document.getElementById "app")))

(defn- listen-keyboard-shortcuts [evt]
  (when (and (.-ctrlKey evt) (= 13 (.-keyCode evt)))
    (rf/dispatch [:evt.keyboard/ctrl-enter])))

(defn ^:export init []
  (js/window.addEventListener "keydown" listen-keyboard-shortcuts)
  (rf/dispatch-sync [:evt.db/init default-db])
  (mount-root))

;; This is called every time you make a code change
(defn ^:after-load on-reload []
  (css/remove-styles!)
  (mount-root))
