(ns {{root-ns}}.frontend.subs
    (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::greetings
 (fn [db _]
   (:greetings db)))

(reg-sub
 ::greeting-index
 (fn [db _]
   (:greeting-index db)))
