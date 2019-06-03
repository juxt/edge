(ns juxt.crux-ui.frontend.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :subs.query/input  #(:db.query/input  % nil))
(rf/reg-sub :subs.query/result #(:db.query/result % nil))
(rf/reg-sub :subs.query/error  #(:db.query/error  % nil))


