(ns juxt.crux-ui.frontend.subs
  (:require [re-frame.core :as rf]
            [juxt.crux-ui.frontend.logic.query-analysis :as qa]
            [cljs.reader]))

(rf/reg-sub :subs.query/input-committed  (fnil :db.query/input-committed  false))
(rf/reg-sub :subs.query/input  (fnil :db.query/input  false))
(rf/reg-sub :subs.query/result (fnil :db.query/result false))
(rf/reg-sub :subs.query/error  (fnil :db.query/error  false))

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
  :subs.query/info
  :<- [:subs.query/input-edn-committed]
  (fn [input-edn]
    (cond
      (not input-edn)      nil
      (:error input-edn)   nil
      :else (qa/calc-query-info input-edn))))

(rf/reg-sub
  :subs.query/headers
  :<- [:subs.query/result]
  :<- [:subs.query/info]
  (fn [[q-res q-info]]
    (when q-info
      (if (:full-results? q-info)
         (qa/analyze-full-results-headers q-res)
         (:find q-info)))))


(rf/reg-sub
  :subs.query/results-table
  :<- [:subs.query/headers]
  :<- [:subs.query/info]
  :<- [:subs.query/result]
  (fn [[q-headers q-info q-res :as args]]
    (when (every? some? args)
      {:headers q-headers
       :rows (if (:full-results? q-info)
               (->> q-res
                    (map :crux.query/doc)
                    (map #(map % q-headers)))
               [q-res])})))


























