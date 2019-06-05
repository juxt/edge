(ns juxt.crux-ui.frontend.subs
  (:require [re-frame.core :as rf]
            [medley.core :as m]
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

(defn calc-vector-headers [query-vector]
  (->> query-vector
       (drop-while #(not= :find %))
       (rest)
       (take-while #(not= (keyword? %)))))

(defn analyze-results-headers [query-results-seq]
  (let [res-count (count query-results-seq)
        sample (if (> res-count 50)
                 (random-sample (/ 50 res-count) query-results-seq)
                 query-results-seq)]
    (set (map keys sample))))

(defn query-vec->map [qv]
  (let [raw-map
          (->> qv
               (partition-by keyword?)
               (partition 2)
               (into {}))]
    (-> raw-map
        (update :find vec)
        (m/update-existing :full-results? first)
        (update :where vec))))

(defn calc-query-info [input-edn]
  (if (vector? input-edn)
    (query-vec->map input-edn)
    input-edn))

(rf/reg-sub
  :subs.query/info
  :<- [:subs.query/input-edn]
  (fn [input-edn]
    (cond
      (not input-edn)      nil
      (:error input-edn)   nil
      :else (calc-query-info input-edn))))

(rf/reg-sub
  :subs.query/headers
  :<- [:subs.query/result]
  :<- [:subs.query/info]
  (fn [[q-res q-info]]
    (when q-info
      (if (:full-results? q-info)
         (analyze-results-headers q-res)
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
               (map #(map % q-headers) q-res)
               q-res)})))


























