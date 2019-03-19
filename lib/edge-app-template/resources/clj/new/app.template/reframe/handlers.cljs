(ns {{root-ns}}.frontend.handlers
  (:require
    [re-frame.core :as rf]
    [{{root-ns}}.frontend.db :as db]))

;; -- Handlers --------------------------------------------------------------

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/app-db))

(rf/reg-event-db
 ::set-random-greeting-index
 (fn [db _]
   (assoc db :greeting-index (rand-int 4))))
