(ns juxt.crux-ui.server.main
  (:require
    crux.api
    [yada.yada :as yada]
    [integrant.core :as ig]
    [crux.api :as api]
    [crux.backup :as backup]
    [crux.http-server :as srv]
    [crux.codec :as c]
    [crux.io :as crux-io]
    [crux.decorators.aggregation.alpha :as aggr]
    [crux.query :as q]
    [clojure.instant :as instant]
    [clojure.tools.logging :as log]
    [clojure.pprint :as pp]
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [yada.yada :refer [handler listener]]
    [hiccup2.core :refer [html]]
    [hiccup.util]
    [yada.resource :refer [resource]]
    [yada.resources.classpath-resource]
    [cheshire.core :as json]
    [clojure.tools.namespace.repl :as c.t.n.r]
    [clojure.java.io :as io]
    [clojure.java.shell :refer [sh]]
    [yada.yada :as yada]
    [integrant.core :as ig]))

(def id #uuid "50005565-299f-4c08-86d0-b1919bf4b7a9")

(defn- page-head [title]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "Content-Language" :content "en"}]
   [:meta {:name "google" :content "notranslate"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/normalize.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/main.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/codemirror.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/styles/monokai.css"}]
 ; [:script {:src "/cljsjs/production/vega.min.inc.js"}]
 ; [:script {:src "/cljsjs/production/vega-lite.min.inc.js"}]
 ; [:script {:src "/cljsjs/production/vega-embed.min.inc.js"}]
   [:title title]])

(defn- gen-console-page [ctx]
    (str
      "<!DOCTYPE html>"
      (html
        [:html {:lang "en"}
         (page-head "Crux Console")
         [:body
          [:div#app
           [:style
            "

html, body, #app, .preloader {
  width: 100%;
  height: 100%;
}

.preloader {
  display: flex;
  align-items: center;
  justify-content: center;
}

.scene {
  width: 200px;
  height: 200px;
  perspective: 600px;
}

.cube {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transform: translateZ(-100px) rotateX(-45deg) rotateY(-45deg);
}

.cube__face {
  position: absolute;
  width: 200px;
  height: 200px;
  border: 1px solid orange;
}

.cube__face--front  { transform: rotateY(  0deg); }
.cube__face--right  { transform: rotateY( 90deg); }
.cube__face--back   { transform: rotateY(180deg); }
.cube__face--left   { transform: rotateY(-90deg); }
.cube__face--top    { transform: rotateX( 90deg); }
.cube__face--bottom { transform: rotateX(-90deg); }


.cube__face--front  { transform: rotateY(  0deg) translateZ(100px); }
.cube__face--right  { transform: rotateY( 90deg) translateZ(100px); }
.cube__face--back   { transform: rotateY(180deg) translateZ(100px); }
.cube__face--left   { transform: rotateY(-90deg) translateZ(100px); }
.cube__face--top    { transform: rotateX( 90deg) translateZ(100px); }
.cube__face--bottom { transform: rotateX(-90deg) translateZ(100px); }

.cube__face.cube__face--back {}

.cube__face {
    border-color: gray !important;
}


@keyframes pulse-bottom-left {
    0% {
        border-bottom: 1px solid gray;
        border-left: 1px solid gray;
    }
    25% {
        border-bottom: 1px solid orange;
        border-left: 1px solid orange;
    }
    50% {
        border-left: 1px solid gray;
        border-top: 1px solid gray;
    }
}
@keyframes pulse-bottom-right {
    0% {
        border-bottom: 1px solid gray;
        border-right: 1px solid gray;
    }
    25% {
        border-bottom: 1px solid orange;
        border-right: 1px solid orange;
    }
    50% {
        border-left: 1px solid gray;
        border-top: 1px solid gray;
    }
}
@keyframes pulse-top-right {
    0% {
        border-top: 1px solid gray;
        border-right: 1px solid gray;
    }
    25% {
        border-top: 1px solid orange;
        border-right: 1px solid orange;
    }
    50% {
        border-left: 1px solid gray;
        border-top: 1px solid gray;
    }
}
@keyframes pulse-top-left {
    0% {
        border-left: 1px solid gray;
        border-top: 1px solid gray;
    }
    25% {
        border-left: 1px solid orange;
        border-top: 1px solid orange;
    }
    50% {
        border-left: 1px solid gray;
        border-top: 1px solid gray;
    }
}


.cube__face--front {
    animation: pulse-top-right 8s 0s infinite, pulse-bottom-right 8s 2s infinite;
}
.cube__face--right {
     animation: pulse-top-left 8s 0s infinite, pulse-bottom-left 8s 2s infinite;
}
.cube__face--top {
     animation: pulse-bottom-right 8s 0s infinite, pulse-top-left 8s 6s infinite;
}
.cube__face--bottom {
     animation: pulse-top-right 8s 2s infinite, pulse-bottom-left 8s 4s infinite;
}
.cube__face--back {
    animation: pulse-bottom-right 8s 4s infinite, pulse-top-right 8s 6s infinite;
}
.cube__face--left {
     animation: pulse-bottom-left 8s 4s infinite, pulse-top-left 8s 6s infinite;
}



"
            ]
           [:div.preloader
            [:div.scene
             (let [titles? false]
             [:div.cube
              [:div.cube__face.cube__face--front  (if titles? "front")]
              [:div.cube__face.cube__face--back   (if titles? "back")]
              [:div.cube__face.cube__face--right  (if titles? "right")]
              [:div.cube__face.cube__face--left   (if titles? "left")]
              [:div.cube__face.cube__face--top    (if titles? "top")]
              [:div.cube__face.cube__face--bottom (if titles? "bottom")]])]]]
        ; [:script {:src "/static/crux-ui/compiled/app.js"}]
        #_[:script {:type "text/javascript"}
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
                  [:a {:href "mailto:crux@juxt.pro"} [:span.n "crux@juxt.pro"]]
                  ]
                 ]
                ]
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
