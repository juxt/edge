(ns juxt.crux-ui.frontend.events.facade
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :evt.db/init
  (fn [_ [_ db]] db))

(rf/reg-event-db
  :evt.ui/query-change
  (fn [db [_ query-text]]
    (println :evt.ui/query-change query-text)
    (println db)
    (assoc db :db.query/input query-text)))
