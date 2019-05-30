(ns juxt.crux-ui.frontend.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :init-db
  (fn [_ _]
    {}))
