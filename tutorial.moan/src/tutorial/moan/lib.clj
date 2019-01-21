(ns tutorial.moan.lib
  (:require
    [tutorial.moan.db :as db]))

(defn favorites
  []
  [{:id 1
    :text "This is a fantastic tweet"
    :author {:name "Elon Musk"
             :username "fakeelonmusk"}
    :favorite? true}
   {:id 2
    :text "Lord but I dislike poetry. How can anyone remember words that aren't put to music?"
    :author {:name "Arliden"
             :username "arl_the_bard"}
    :favorite? true}])

(defn all
  []
  [{:id 1
    :text "I'm loving Hicada"
    :author {:name "Dominic Monroe"
             :username "overfl0w"}
    :favorite? true}
   {:id 2
    :text "Vim is not a lisp editor!!!1"
    :author {:name "Malcolm Sparks"
             :username "sparks0id"}}
   {:id 3
    :text "jskjfksjuf828hsdfj"
    :author {:name "Prince"
             :username "spamlord"}
    :hidden? true}])

(defn toggle-favorite
  [{:keys [tweet-id]}]
  (println "Adding favorite to " tweet-id))
