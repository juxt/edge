(ns juxt.crux-ui.server.main
  (:require
    crux.api
    [yada.yada :as yada]
    [integrant.core :as ig]
    [yada.yada :refer [handler listener]]
    [hiccup2.core :refer [html]]
    [hiccup.util]
    [yada.resource :refer [resource]]
    [yada.resources.classpath-resource]
    [clojure.java.shell :refer [sh]]
    [juxt.crux-ui.server.preloader :as preloader]
    [yada.yada :as yada]
    [integrant.core :as ig]))

(def id #uuid "50005565-299f-4c08-86d0-b1919bf4b7a9")

(defn- page-head [title]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "Content-Language" :content "en"}]
   [:meta {:name "google" :content "notranslate"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/reset.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/codemirror.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/monokai.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/eclipse.css"}]
 ; [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/main.css"}]
 ; [:script {:src "/cljsjs/production/vega.min.inc.js"}]
 ; [:script {:src "/cljsjs/production/vega-lite.min.inc.js"}]
 ; [:script {:src "/cljsjs/production/vega-embed.min.inc.js"}]
   [:title title]
   [:style#_stylefy-constant-styles_]
   [:style#_stylefy-styles_]
   ])

(defn- gen-console-page [ctx]
    (str
      "<!DOCTYPE html>"
      (html
        [:html {:lang "en"}
         (page-head "Crux Console")
         [:body
          [:div#app preloader/root]
          [:script {:src "/static/crux-ui/compiled/app.js"}]
          [:script {:type "text/javascript"}
           (hiccup.util/raw-string
             "console.log('calling init');"
             "juxt.crux_ui.frontend.main.init()")]]])))

(defmethod ig/init-key ::console
  [_ {:keys [system]}]
  (yada/resource
    {:id ::console
     :methods
         {:get
          {:produces ["text/html" "application/edn" "application/json"]
           :response gen-console-page}}}))


(defmethod ig/init-key ::home
  [_ {:keys [system]}]
  (yada/resource
    {:id ::home
     :methods
     {:get
      {:produces ["text/html" "application/edn" "application/json"]
       :response
       (fn [ctx]
         (let []
         (str
           "<!DOCTYPE html>"
           (html
             [:html {:lang "en"}
              (page-head "Crux Standalone Demo with HTTP")
              [:body
               [:header
                [:div.nav
                 [:div.logo {:style {:opacity "0"}} [:a {:href "/"} [:img.logo-img {:src "/static/img/crux-logo.svg"}]]]
                 [:div.n0
                  [:a {:href "https://juxt.pro/crux/docs/index.html"} [:span.n "Documentation"]]
                  [:a {:href "https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux"} [:span.n "Community Chat"]]
                  [:a {:href "mailto:crux@juxt.pro"} [:span.n "crux@juxt.pro"]]]]]
               [:div {:style {:text-align "center" :width "100%" :margin-top "6em"}}
                   [:div.splash {:style {:max-width "25vw" :margin-left "auto" :margin-right "auto"}} [:a {:href "/"} [:img.splash-img {:src "/static/img/crux-logo.svg"}]]]

               [:div {:style {:height "4em"}}]
               [:h3 "You should now be able to access this Crux standalone demo system using the HTTP API via localhost:8080"]
                   ]
               [:div#app]
               ]]))))}}}))

(defmethod ig/init-key ::read-write
  [_ {:keys [system]}]
  (yada/resource
    {:id ::read-write
     :methods
     {:get
      {:produces ["text/html" "application/edn" "application/json"]
       :response (fn [ctx]
                   (let [db (crux.api/db system)]
                     (map
                       #(crux.api/entity db (first %))
                       (crux.api/q
                         db
                         {:find '[?e]
                          :where [['?e :crux.db/id id]]}))))}
      :post
      {:produces "text/plain"
       :consumes "application/edn"
       :response
       (fn [ctx]
         (crux.api/submit-tx
           system
           [[:crux.tx/put id
             (merge {:crux.db/id id} (:body ctx))]])
         (yada/redirect ctx ::read-write))}}}))

;; To populate data using cURL:
; $ curl -H "Content-Type: application/edn" -d '{:foo/username "Bart"}' localhost:8300/rw
