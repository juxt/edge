;; Copyright © 2016, JUXT LTD.

(ns edge.webserver
  (:require
   [aleph.http :as http]
   [bidi.ring :refer [make-handler redirect]]
   [hiccup.core :refer [html]]
   [clojure.core.async :refer [chan >!! mult tap close! go-loop <! >! timeout]]
   [clojure.java.io :as io]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [manifold.deferred :as d]
   [schema.core :as s]
   [cheshire.core :as json]
   [byte-streams :as b]
   [om.next.server :as om]
   [yada.yada :refer [yada resource]]))

(def URI "http://live-cdn.me-tail.net/cantor/api/wanda/garment-details/81e36a81-1921-4247-a8de-1ed7eb67840f?skus=nct_sandbox_dress_37_7cpxa7,nct_sandbox_trousers_2_5bahk2,nct_sandbox_jacket_4_btcjsf,nct_sandbox_coat_3_vxbetj,nct_sandbox_top_24_7k94td,nct_sandbox_skirt_15_04s5md,nct_sandbox_top_15_zwzenw")

(defn get-garments [uri]
  {:garments/by-id
   (let [response (deref (http/get uri))]
     (if (= (:status response) 200)
       (into {}
             (for [garment (json/decode (b/to-string (:body response)) keyword)]
               [(:id garment) (conj garment [:likes (rand-int 40)])]))
       (throw (Exception. "Oh no!!!"))))})

(defn get-garments-async [uri]
  (d/chain
   (http/get uri)
   (fn [response]
     (if (= (:status response) 200)
       {:garments/by-id
        (into {}
              (for [garment (json/decode (b/to-string (:body response)) keyword)]
                [(:id garment) (assoc garment :likes (rand-int 40))]
                ))}
       (throw (Exception. "Oh no!!!"))))))

(defn readf [env k params]
  (infof "Reading k is %s" k)
  (infof "env is %s" (dissoc env :state))
  (infof "query is <%s>" (:query env))

  (let [st @(:state env)]
    (infof "state keys are %s" (keys st))
    (infof "garments are %s"  (:garments/by-id st))
    (case k
      :garments/by-id
      {:value (into {} ; just pick the garment properties we're being asked for
                    (for [[id garment] (:garments/by-id st)]
                      [id (select-keys garment (:query env))]
                      ))}
      

      (if-let [[_ v] (find st k)]
        {:value v}
        {:value :not-found}))))

(defn mutatef [env k params]
  (infof "MUTATE! %s" k)
  {})

(defn om-resource [parser app-state]
  (infof "app-state is %s" @app-state)
  (resource
   {:methods
    {:post
     {:consumes "application/transit+json"
      :produces "application/transit+json"
      :response (fn [ctx]
                  (infof "query is %s" (:body ctx))
                  (let [msg
                        (parser {:state app-state} (:body ctx))]
                    (infof "msg is %s" msg)
                    msg
                    ))}}}))

(defn create-api [parser app-state mlt]
  ["/"                   
   [
    ["hello" (yada "hello")]

    ["newsfeed" (yada (resource {:methods
                                 {:get
                                  {:produces "text/event-stream"
                                   :response mlt}
                                  }}))]
    
    ["garments" (yada (get-garments URI))]
    
    ["garments-async" (yada
                       (resource
                        {:methods
                         {:get {:produces "text/html"
                                :response (fn [ctx] (get-garments-async URI))}}}))]    

    ["api" (yada (om-resource parser app-state))]
    ["" (redirect "index.html")]
    ["favicon.ico" (yada nil)]
    ["" (yada (io/file "target/dev"))]
    [true (yada nil)] ; 404 everything else
    ]])

(chan 10)

(let [c (chan 100)]
  )






(s/defrecord Webserver [port :- (s/pred integer? "must be a port number!!")
                        app-state
                        server
                        mlt
                        ch
                        api]
  Lifecycle
  (start [component]
    (let [app-state (atom @(get-garments-async URI))
          ch (chan 100)
          mlt (mult ch)
          spychannel (chan 10)
          api (create-api (om/parser {:read readf :mutate mutatef}) app-state mlt)
          ]
      (tap mlt spychannel)
      
      (go-loop []
        (<! (timeout 1000))
        (when
            (>! ch (rand-nth ["Special offer on Santa costumes! They now free of charge!"
                              "Super sale 50% discount on all items!!!"
                              "Discount Wednesday today!!!!"
                              "metail make profits this year!!!"]))
          (recur)))

      (go-loop []
        (when-let [v (<! spychannel)]
          (infof "news! %s" v)
          (recur)))
      
      (assoc component
             :app-state app-state
             :server (http/start-server (make-handler api) component)
             :ch ch
             :mlt mlt
             :api api)))
  
  (stop [component]
    (when-let [server (:server component)] (.close server))
    (when-let [ch (:ch component)] (close! ch))
    component))

(defn new-webserver []
  (using
   (map->Webserver {:port 3000})
   []))

