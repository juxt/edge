(ns tutorial.vent.lib
  (:require
    [tutorial.vent.db :as db]))

(defn favorites
  []
  [{:id 1
    :text "A favorited tweet"
    :author {:name "John smith"}
    :username "john_smith"
    :favorite? true}
   {:id 2
    :text "Another favorite tweet"
    :author {:name "Jane Smith"}
    :username "jane_smith"
    :favorite? true}])

(defn all
  []
  [{:id 1
    :text "A hardcoded tweet"
    :author {:name "John Smith"}
    :username "john_smith"
    :favorite? true}
   {:id 2
    :text ""
    :author {:name "Jane Smith"}
    :username "jane_smith"}])

(defn followers
  [{:keys [user]}]
  {"john_smith"
   {:name "John Smith"}
   "jane_smith"
   {:name "Jane Smith"
    :following? true}})

(defn following
  [{:keys [user]}]
  {"jane_smith"
   {:name "Jane Smith"
    :following? true}})

(defn toggle-favorite
  [{:keys [vent-id username]}]
  (println username "is toggling favorite on" vent-id))

(defn add-vent
  [{:keys [text username]}]
  (println username "is venting about" text))

(defn toggle-follow
  [{:keys [to-follow username]}]
  (println username "is follwoing or unfollowing" to-follow))
