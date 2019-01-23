(ns tutorial.vent.lib
  (:require
    [tutorial.vent.db :as db]))

(defn favorites
  []
  [{:id 1
    :text "This is a fantastic tweet"
    :author {:name "Elon Musk"}
    :username "fakeelonmusk"
    :favorite? true}
   {:id 2
    :text "Lord but I dislike poetry. How can anyone remember words that aren't put to music?"
    :author {:name "Arliden"}
    :username "arl_the_bard"
    :favorite? true}])

(defn all
  []
  [{:id 1
    :text "I'm loving Hicada"
    :author {:name "Dominic Monroe"}
    :username "overfl0w"
    :favorite? true}
   {:id 2
    :text "Vim is not a lisp editor!!!1"
    :author {:name "Malcolm Sparks"}
    :username "sparks0id"}
   {:id 3
    :text "jskjfksjuf828hsdfj"
    :author {:name "Prince"}
    :username "spamlord"
    :hidden? true}])

(defn followers
  []
  {"overfl0w"
   {:id 1
    :name "Dominic Monroe"}
   "sparks0id"
   {:id 2
    :name "Malcolm Sparks"
    :following? true}})

(defn following
  []
  {"overfl0w"
   {:id 1
    :name "Dominic Monroe"
    :following? true}
   "arl_the_bard"
   {:id 2
    :name "Arliden"
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
