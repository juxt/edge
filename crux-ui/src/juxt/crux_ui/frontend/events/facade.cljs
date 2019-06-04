(ns juxt.crux-ui.frontend.events.facade
  (:require [re-frame.core :as rf]
            [juxt.crux-ui.frontend.io.query :as q]))



; ----- effects -----

(rf/reg-fx
  :fx/query-exec
  (fn [query-text]
    (q/exec query-text)))

(rf/reg-fx
  :fx/query-stats
  (fn [_]
   ;(q/fetch-stats)
    ))



; ----- events -----

(rf/reg-event-fx
  :evt.db/init
  (fn [_ [_ db]]
    {:db db
    ;:fx/query-stats nil
     }))

(rf/reg-event-db
  :evt.io/stats-success
  (fn [db [_ stats]]
    (assoc db :db.meta/stats stats)))

(rf/reg-event-db
  :evt.io/query-success
  (fn [db [_ res]]
    (assoc db :db.query/result res)))

(rf/reg-event-fx
  :evt.keyboard/ctrl-enter
  (fn []
    {:dispatch [:evt.ui/query-submit]}))

(rf/reg-event-fx
  :evt.ui/query-submit
  (fn [{:keys [db] :as ctx}]
    {:db db
     :fx/query-exec (:db.query/input db)}))

(rf/reg-event-db
  :evt.ui/query-change
  (fn [db [_ query-text]]
    (assoc db :db.query/input query-text)))
