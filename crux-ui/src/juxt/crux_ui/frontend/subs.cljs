(ns juxt.crux-ui.frontend.subs
  (:require [re-frame.core :as rf]
            [cljs.reader]))

(rf/reg-sub :subs.query/input-committed  (fnil :db.query/input-committed  nil))
(rf/reg-sub :subs.query/input  (fnil :db.query/input  nil))
(rf/reg-sub :subs.query/result (fnil :db.query/result nil))
(rf/reg-sub :subs.query/error  (fnil :db.query/error  nil))

(rf/reg-sub :subs.query/input-edn-committed
            :<- [:subs.query/input-committed]
            (fn [input-str]
              (try
                (cljs.reader/read-string input-str)
                (catch js/Error e
                  {:error e}))))

(rf/reg-sub :subs.query/input-edn
            :<- [:subs.query/input]
            (fn [input-str]
              (try
                (cljs.reader/read-string input-str)
                (catch js/Error e
                  {:error e}))))

(rf/reg-sub :subs.query/input-malformed?
            :<- [:subs.query/input-edn]
            #(:error % false))

(rf/reg-sub
  :subs.query/headers
  :<- [:subs.query/result]
  :<- [:subs.query/input-edn]
  (fn [[q-res input-edn]]
    {}))
