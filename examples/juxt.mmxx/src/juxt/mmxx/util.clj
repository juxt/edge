;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.util
  (:require
   [juxt.spin.alpha.server :as spin.server]
   [juxt.flux.helpers :as a]
   [juxt.flow.protocols :as flow]))

(defn stream-request-body-to-file [server-provider request on-complete raise]
  (let [vertx (:juxt.flux/vertx request)
        fs (. vertx fileSystem)]
    (fn [path]
      (. fs open path (new io.vertx.core.file.OpenOptions)
         (a/har
          {:on-success
           (fn [async-file]
             (let [delegate (.toSubscriber async-file)]
               (spin.server/subscribe-to-request-body
                server-provider
                request
                (reify flow/Subscriber
                  (on-subscribe [_ subscription]
                    (.onSubscribe
                     delegate
                     (reify org.reactivestreams.Subscription
                       (cancel [_] (flow/cancel subscription))
                       (request [_ n] (flow/request subscription n)))))
                  (on-error [_ t]
                    (raise (ex-info "Failed to stream request body to file" {} t)))
                  (on-next [_ item]
                    (.onNext delegate item))
                  (on-complete [_]
                    ;; Calling onComplete seems to also close the file
                    (.onComplete delegate)
                    (println "Successfully written to" path)
                    (on-complete))))))
           :on-failure
           (fn [cause]
             (raise (ex-info "Failed to open temporary file for writing" {:path path} cause)))})))))


(defn wrap-temp-file [streamer prefix suffix request _ raise]
  (let [vertx (:juxt.flux/vertx request)
        fs (. vertx fileSystem)]
    (. fs createTempFile prefix suffix
       (a/har
        {:on-success
         streamer
         :on-failure                      ; failed to create
         (fn [cause]
           (raise (ex-info "Failed to create temporary file" {:suffix suffix} cause)))}))))
