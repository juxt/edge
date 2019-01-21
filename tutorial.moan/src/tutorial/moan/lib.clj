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
