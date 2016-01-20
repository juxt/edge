(ns edge.main
  (:require
   [reagent.core :as reagent])
  )

(def db (reagent/atom {:title "Chat!"
                       :messages []}))

(defn add-message [msg]
  (swap! db update :messages conj msg))

(defn title []
  [:h1 (:title @db)])

(defn messages []
  [:div
   [:table
    [:tbody
     (for [msg (take 20 (reverse (:messages @db)))]
       [:tr
        ^{:key msg} [:td msg]])]]])

(defn post-message []
  (let [formdata (new js/FormData (.getElementById js/document "foo"))
        x (new js/XMLHttpRequest)]
    (.open x "POST" "http://localhost:3000/chat.html")
    (.send x formdata)
    ;;(set! (.-onload x) (fn [evt]))
    )
  )

(defn inputbox [title]
  [:div
   [:h2 title]
   [:form#foo {:method "POST"}
    [:div
     [:label "Topic"] [:input {:type :text :name "topic"}]]
    [:div [:label "Message"] [:input {:type :text :name "message"}]]
    [:button {:on-click (fn [evt]
                          (.preventDefault evt)
                          (post-message))} "Submit"]]])

(defn chat []
  [:div
   [title]
   [messages]
   [inputbox "Enter your message:"]
   ])

(defonce es (new js/EventSource "http://localhost:3000/firehose"))

(defn init "The main entry point" []
  (enable-console-print!)

  (reagent/render [chat] (.getElementById js/document "content"))
  
  (set! (.-onmessage es)
        (fn [evt] (add-message evt.data)))

  (reagent/force-update-all))


