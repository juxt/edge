(ns {{name}}.subs
    (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::greetings
 (fn [db _]
   (:greeting db)))

(reg-sub
 ::greeting-index
 (fn [db _]
   (:greeting-index db)))
