(ns ^:figwheel-hooks tutorial.moan.frontend.main
  (:require-macros
    [tutorial.moan.frontend.main :refer [html]])
  (:require
    [brutha.core :as br]
    [cljs.reader :refer [read-string]]))

(defonce state (atom {:page {:name :home}}))

(declare fetch-global-page)

(defn update-tweet
  [tweets id f & args]
  (map
    (fn [tweet]
      (if (= id (:id tweet))
        (apply f tweet args)
        tweet))
    tweets))

(def Tweet
  (br/component
    'Tweet
    (fn [tweet]
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
                       (->
                         (js/fetch (str "/" (:id tweet) "/favorite")
                                   #js {:method "POST"})
                         (.then (fn [_]
                                  (fetch-global-page)))))}
           [:span.star.tweet__action-icon]
           "Favorite"]]]))))

(defn Tweets
  [tweets]
  (html
    [:div.tweets
     (for [tweet tweets]
       (when-not (:hidden? tweet)
         (Tweet tweet {:key (:id tweet)})))]))

(def User
  (br/component
    'User
    (fn [user]
      (html
        [:div.card
         [:h2 (:name user)]
         [:h3 (:username user)]]))))

(defn Users
  [users]
  (html
    [:div.tweets
     (for [user users]
       (User user {:key (:id user)}))]))

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
    (Tweets (-> state :page :data))))

(defn favorites
  [state]
  (html (Tweets (-> state :page :data))))

(defn followers
  [state]
  (html (Users (-> state :page :data))))

(defn following
  [state]
  (html (Users (-> state :page :data))))

(defmulti page-fetch :name)

(defmethod page-fetch :favorites
  [_]
  (js/fetch "/favorites"))

(defmethod page-fetch :home
  [_]
  (js/fetch "/all"))

(defmethod page-fetch :followers
  [_]
  (js/fetch "/followers"))

(defmethod page-fetch :following
  [_]
  (js/fetch "/following"))

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

(defn fetch-global-page
  []
  (fetch-page (-> @state :page)))

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
          :followers
          (followers state)
          :following
          (following state)

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
(defonce start-up (do (init @state) true))

(add-watch state
           ::rerender
           (fn [_ _ old-state state]
             (mount state)))

(comment
  (html [:* [:h1 "Hi"]])
  )
