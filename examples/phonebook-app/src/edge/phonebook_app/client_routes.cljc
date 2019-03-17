(ns edge.phonebook-app.client-routes
  (:require [bidi.bidi :as bidi]))

(def client-routes
  [
   ["" :index]
   [["/" :id] :entry]])
