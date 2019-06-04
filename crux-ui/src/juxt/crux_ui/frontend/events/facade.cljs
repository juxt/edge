(ns juxt.crux-ui.frontend.events.facade
  (:require [re-frame.core :as rf]
            [juxt.crux-ui.frontend.io.query :as q]))

(rf/reg-fx
  :fx/query-exec
  (fn [query-text]
    (q/exec query-text)))

(rf/reg-event-db
  :evt.db/init
  (fn [_ [_ db]] db))

(rf/reg-event-db
  :evt.io/query-success
  (fn [db [_ res]]
    (assoc db :db.query/result res)))

(rf/reg-event-fx
  :evt.ui/query-submit
  (fn [{:keys [db] :as ctx}]
    {:db db
     :fx/query-exec (:db.query/input db)}))

(rf/reg-event-db
  :evt.ui/query-change
  (fn [db [_ query-text]]
    (assoc db :db.query/input query-text)))
