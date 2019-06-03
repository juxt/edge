(ns juxt.crux-ui.frontend.events.facade
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :evt.db/init
  (fn [_ db]
    db))
