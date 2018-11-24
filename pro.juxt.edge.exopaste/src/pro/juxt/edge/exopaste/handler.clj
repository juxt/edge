(ns pro.juxt.edge.exopaste.handler
  (:require
    [integrant.core :as ig]
    [yada.yada :as yada]
    [pro.juxt.edge.exopaste.view :as view]
    [pro.juxt.edge.exopaste.store :as store]))

(defn index
  [store]
  (yada/resource
    {:id ::index
     :methods
     {:get
      {:produces {:media-type "text/html"
                  :charset "utf-8"}
       :response
       (fn [ctx]
         (view/render-form))}
      :post
      {:consumes "application/x-www-form-urlencoded"
       :parameters {:form {:content String}}
       :produces "text/plain"
       :response
       (fn [ctx]
         (let [uuid (store/add-new-paste store (get-in ctx [:parameters :form :content]))]
           (new java.net.URI (:href (yada/uri-info ctx ::paste {:route-params {:uuid uuid}})))))}}}))

(defn paste
  [store]
  (yada/resource
    {:id ::paste
     :methods
     {:get
      {:produces {:media-type "text/html"
                  :charset "utf-8"}
       :response
       (fn [ctx]
         (when-let [paste (store/get-paste-by-uuid store (get-in ctx [:parameters :path :uuid]))]
           (view/render-paste paste)))}}}))

(defmethod ig/init-key ::index
  [_ {:keys [store]}]
  (index store))

(defmethod ig/init-key ::paste
  [_ {:keys [store]}]
  (paste store))
