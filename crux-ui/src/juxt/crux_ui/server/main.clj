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
   [:script {:src "/cljsjs/production/vega.min.inc.js"}]
   [:script {:src "/cljsjs/production/vega-lite.min.inc.js"}]
   [:script {:src "/cljsjs/production/vega-embed.min.inc.js"}]
 ; [:script {:src "/cljsjs/codemirror/production/codemirror.min.inc.js"}]
 ; [:script {:src "/cljsjs/codemirror/common/mode/clojure.inc.js"}]
 ; [:script {:src "/cljsjs/codemirror/common/keymap/emacs.inc.js"}]
 ; [:script {:src "/cljsjs/codemirror/common/addon/edit/closebrackets.inc.js"}]
 ; [:script {:src "/cljsjs/codemirror/common/addon/edit/matchbrackets.inc.js"}]
   ;from https://wzrd.in/standalone/uuid%2Fv4@latest
    [:script {:type "text/javascript"}
     (hiccup.util/raw-string
"!function(e){if('object'==typeof exports&&'undefined'!=typeof module)module.exports=e();else if('function'==typeof define&&define.amd)define([],e);else{('undefined'!=typeof window?window:'undefined'!=typeof global?global:'undefined'!=typeof self?self:this).uuidv4=e()}}(function(){return function(){return function e(n,r,t){function o(f,u){if(!r[f]){if(!n[f]){var a='function'==typeof require&&require;if(!u&&a)return a(f,!0);if(i)return i(f,!0);var p=new Error('Cannot find module '+f+'');throw p.code='MODULE_NOT_FOUND',p}var y=r[f]={exports:{}};n[f][0].call(y.exports,function(e){return o(n[f][1][e]||e)},y,y.exports,e,n,r,t)}return r[f].exports}for(var i='function'==typeof require&&require,f=0;f<t.length;f++)o(t[f]);return o}}()({1:[function(e,n,r){for(var t=[],o=0;o<256;++o)t[o]=(o+256).toString(16).substr(1);n.exports=function(e,n){var r=n||0,o=t;return[o[e[r++]],o[e[r++]],o[e[r++]],o[e[r++]],'-',o[e[r++]],o[e[r++]],'-',o[e[r++]],o[e[r++]],'-',o[e[r++]],o[e[r++]],'-',o[e[r++]],o[e[r++]],o[e[r++]],o[e[r++]],o[e[r++]],o[e[r++]]].join('')}},{}],2:[function(e,n,r){var t='undefined'!=typeof crypto&&crypto.getRandomValues&&crypto.getRandomValues.bind(crypto)||'undefined'!=typeof msCrypto&&'function'==typeof window.msCrypto.getRandomValues&&msCrypto.getRandomValues.bind(msCrypto);if(t){var o=new Uint8Array(16);n.exports=function(){return t(o),o}}else{var i=new Array(16);n.exports=function(){for(var e,n=0;n<16;n++)0==(3&n)&&(e=4294967296*Math.random()),i[n]=e>>>((3&n)<<3)&255;return i}}},{}],3:[function(e,n,r){var t=e('./lib/rng'),o=e('./lib/bytesToUuid');n.exports=function(e,n,r){var i=n&&r||0;'string'==typeof e&&(n='binary'===e?new Array(16):null,e=null);var f=(e=e||{}).random||(e.rng||t)();if(f[6]=15&f[6]|64,f[8]=63&f[8]|128,n)for(var u=0;u<16;++u)n[i+u]=f[u];return n||o(f)}},{'./lib/bytesToUuid':1,'./lib/rng':2}]},{},[3])(3)});"
)]
   ;[:script {:src "/cljs-out/dev-main.js"}]
   [:title title]])

(defmethod ig/init-key ::console
  [_ {:keys [system]}]
  (yada/resource
    {:id ::console
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
                (page-head "Crux Console")
                [:body
                 [:header
                  [:div.nav
                   [:div.logo [:a {:href "/"} [:img.logo-img {:src "/static/img/console-logo.svg"}]]]
                   [:div.n0
                    [:a {:href "https://juxt.pro/crux/docs/index.html"} [:span.n "Documentation"]]
                    [:a {:href "https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux"} [:span.n "Community Chat"]]
                    [:a {:href "mailto:crux@juxt.pro"} [:span.n "crux@juxt.pro"]]
                    ]
                   ]
                  ]
                 [:div#app]

                 [:script {:src "/static/crux-ui/compiled/app.js"}]
                 [:script {:type "text/javascript"}
                  (hiccup.util/raw-string
                    "console.log('calling init');"
                    "juxt.crux_ui.frontend.main.init()")]]]))))}}}))


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
