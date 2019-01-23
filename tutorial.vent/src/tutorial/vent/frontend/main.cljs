(ns ^:figwheel-hooks tutorial.vent.frontend.main
  (:require-macros
    [tutorial.vent.frontend.main :refer [html]])
  (:require
    [brutha.core :as br]
    [tutorial.vent.frontend.ajax
     :refer [page-fetch fetch-page]
     :as ajax]))

(defonce state (atom {:page {:name :home}}))
(def state* state)

(declare fetch-global-page)

(defn update-tweet
  [tweets id f & args]
  (map
    (fn [tweet]
      (if (= id (:id tweet))
        (apply f tweet args)
        tweet))
    tweets))

(def UserPreview
  (br/component
    'UserPreview
    (fn [{:keys [username user]}]
      (html
        [:div
         [:span.user__name
          (or (:name user) (str "ERROR: Name missing!"))]
         [:a.user__username
          {:href "javascript:;"
           :onClick (fn [e]
                      (.preventDefault e))}
          (str "@" username)]]))))

(def Tweet
  (br/component
    'Tweet
    (fn [tweet]
      (html
        [:div.card.tweet
         [:div.tweet__content
          (UserPreview {:username (:username tweet)
                        :user (:author tweet)})
          [:p.tweet__body (:text tweet)]]
         [:div.card__actions
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

(defn Compose
  []
  (html
    [:div.card.compose
     [:form
      {:onSubmit (fn [e]
                   (.preventDefault e)
                   (.reset (.-target e))
                   (-> (js/fetch "/vent"
                                 #js {:method "PUT"
                                      :body (new js/FormData (.-target e))})
                       (.then
                         (fn [_]
                           (fetch-global-page)))))}
      [:textarea.compose__input
       {:name "text"
        :onKeyDown
        (fn [e]
          (when (and (= (.-keyCode e) 13)
                     (not (.-shiftKey e)))
            (.preventDefault e)
            (.dispatchEvent
              (some-> e (.-target) (.-form))
              (new js/Event "submit" #js {:cancelable true}))))}]
      [:div.card__actions.card__actions--right
       [:button.Button {:type :submit} "Vent"]]]]))

(def User
  (br/component
    'User
    (fn [{:keys [username user] :as value}]
      (html
        [:div.card.tweet
         [:div.card__description
          (UserPreview value)]
         [:div.card__actions.card__actions--right
          [:button.Button
           {:onClick (fn [e]
                       (-> (js/fetch (str "/" username "/follow")
                                     #js {:method "POST"})
                           (.then (fn [_]
                                    (fetch-global-page)))))}
           (if (:following? user)
             "Unfollow"
             "Follow")]]]))))

(defn Users
  [users]
  (html
    [:div.tweets
     (for [[username user] users]
       (User {:username username
              :user user}
             {:key (:id user)}))]))

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
    [:div
     (Compose)
     (Tweets (-> state :page :data))]))

(defn favorites
  [state]
  (html (Tweets (-> state :page :data))))

(defn followers
  [state]
  (html (Users (-> state :page :data))))

(defn following
  [state]
  (html (Users (-> state :page :data))))

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

(defn fetch-global-page
  []
  (fetch-page (-> @state :page) state*))

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
                         (ajax/fetch-page new-page state)))}
        children]])))

(defn root
  [state]
  (html
    [:div.wrapper
     [:nav.primary-nav
      [:span.primary-nav__logo
       "Vent"]
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
  (ajax/fetch-page (-> state :page) state*))

;; and this is what figwheel calls after each save
(defn ^:after-load re-render []
  (init @state))

(defn ^:export re-fetch []
  (ajax/fetch-page (-> @state :page) state*))

;; this only gets called once
(defonce start-up (do (init @state) true))

(add-watch state
           ::rerender
           (fn [_ _ old-state state]
             (mount state)))
