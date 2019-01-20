(ns ^:figwheel-hooks tutorial.moan.frontend.main
  (:require-macros
    [tutorial.moan.frontend.main :refer [html]])
  (:require
    [brutha.core :as br]
    [cljs.reader :refer [read-string]]))

(defonce state (atom {:page {:name :home}
                      :tweets [{:id 1
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
                                :hidden? true}]}))

(defn update-tweet
  [tweets id f & args]
  (map
    (fn [tweet]
      (if (= id (:id tweet))
        (apply f tweet args)
        tweet))
    tweets))

(defn Tweet
  [tweet]
  (html
    [:div.card.tweet
     [:div.tweet__content
      [:span.tweet__author-name (-> tweet :author :name)]
      [:a.tweet__author-username
       {:href "javascript:;"
        :onClick (fn [e]
                   (.preventDefault e))}
       (str "@" (-> tweet :author :username))]
      [:p.tweet__body (:text tweet)]]
     [:div.tweet__actions
      [:a.tweet__action {:href "#"}
       [:span.eye.tweet__action-icon]
       "Hide"]
      [:a.tweet__action
       {:href "#"
        :class (when (:favorite? tweet)
                 "tweet__action--active")
        :onClick (fn [e]
                   (.preventDefault e)
                   (swap! state
                          update :tweets
                          update-tweet (:id tweet)
                          update :favorite? not))}
       [:span.star.tweet__action-icon]
       "Favorite"]]]))

(defn Tweets
  [tweets]
  (html
    [:div.tweets
     (for [tweet tweets]
       (when-not (:hidden? tweet)
         (Tweet tweet)))]))

(defn Loader
  []
  (html
    [:div.Aligner
     [:div.loader
      [:div.loader__box]
      [:div.loader__hill]
      [:p.loader__text "Fetching data…"]]]))

(defn home
  [state]
  (html
    (Tweets (:tweets state))))

(defn favorites
  [state]
  (html (Tweets (-> state :page :data))))

(defmulti page-fetch :name)

(defmethod page-fetch :favorites
  [_]
  (js/fetch "/favorites"))

(defmethod page-fetch :default
  [_]
  nil)

(defn fetch-page
  [page]
  (some-> (page-fetch page)
          (.then (fn [data] (.text data)))
          (.then (fn [text] (read-string text)))
          (.then (fn [x] (swap! state (fn [state]
                                        (if (= page (:page state))
                                          (assoc-in state [:page :data] x)
                                          state)))))))

(defn nav-link
  [opts & children]
  (let [children (if (map? opts)
                   children
                   (cons opts children))]
    (html
      [:li.primary-nav__item
       [:a {:href "javascript:;"
            :onClick (fn [e]
                       (.preventDefault e)
                       (when-let [new-page (:new-page opts)]
                         (swap! state assoc :page new-page)
                         (fetch-page new-page)))}
        children]])))

(defn root
  [state]
  (html
    [:div.wrapper
     [:nav.primary-nav
      [:span.primary-nav__logo
       "Moan"]
      [:ul.primary-nav__list
       (nav-link
         {:new-page {:name :home}}
         "Home")
       (nav-link
         {:new-page {:name :followers}}
         "Followers")
       (nav-link
         {:new-page {:name :following}}
         "Following")
       (nav-link
         {:new-page {:name :favorites}}
         "Favorites")]]
     [:main
      (if (-> state :page :data)
        (case (-> state :page :name)
          :home
          (home state)
          :favorites
          (favorites state)

          [:p.card (str "Hello, " (:foo state "Default"))])
        (Loader))]]))

(defn mount
  [state]
  (br/mount
    (root state)
    (js/document.getElementById "app")))

(defn init
  [state]
  (mount state)
  (fetch-page (-> state :page)))

;; and this is what figwheel calls after each save
(defn ^:after-load re-render []
  (init @state))

;; this only gets called once
(defonce start-up (do (mount @state) true))

(add-watch state
           ::rerender
           (fn [_ _ old-state state]
             (mount state)))

(comment
  (html [:* [:h1 "Hi"]])
  )
