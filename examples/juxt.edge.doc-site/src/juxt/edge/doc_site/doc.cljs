;; Copyright © 2019, JUXT LTD.

(ns ^:figwheel-hooks juxt.edge.doc-site.doc
  (:require
   [goog.dom :as dom]))

;; Create a GET function that can be called by docs

;; Question: How does tick call arbitary functions? buttons/actions?
;; TODO: Run up tick in a separate workspace

(enable-console-print!)

(defn button [label onclick]
  (dom/createDom "button" #js {"onclick" onclick} label))

(defn div [class & body]
  (apply dom/createDom "div" #js {"class" class} body))

(defn pre [class & body]
  (apply dom/createDom "pre" #js {"class" class} body))

(defn p [& body]
  (apply dom/createDom "p" nil body))

(defn tt [& body]
  (apply dom/createDom "tt" nil body))

(defn source-listing [code]
  (div "listingblock"
       (div "content"
            (pre "highlightjs highlight"
                 (dom/createDom "code" #js {"class" "hljs"} code)))))

(defn http
  ([method uri cb]
   (http method uri cb {}))
  ([method uri cb opts]
   (let [req (new js/XMLHttpRequest)]
     (.open req method uri)
     (.setRequestHeader req "Accept" "text/plain")
     (.addEventListener req "load" (fn [ev]
                                     (cb ev req)))
     (.send req))))

(defn init []
  (let [parent (.querySelector js/document "#hello .content")
        output (dom/createDom "div" nil)

        hello
        (fn []
          (http
            "GET" "/hello"
            (fn [ev req]
              (dom/removeChildren output)
              (dom/append output (source-listing
                                   (str
                                     (.getAllResponseHeaders req)
                                     "\r\n"
                                     (.-responseText (.-currentTarget ev))))))))]

    (when parent
      (hello)
      (dom/removeChildren parent)
      (dom/append
       parent
       (p "Let's send a " (tt "GET") " request to " (tt "/hello") " and check what it returns.")
       output
       #_(button "GET" (fn [ev] (hello)))))))

(defonce _
  (do
    (println "Edge: window loaded")
    (init)))

(defn ^:after-load figwheel-reload []
  (println "Edge: figwheel-reload!")
  (init))
