(ns juxt.crux-ui.frontend.views.facade
  (:require-macros [cljss.core :refer [defstyles inject-global]])
  (:require [juxt.crux-ui.frontend.views.query-ui :as q]
            [juxt.crux-ui.frontend.views.header :as header]))

(defstyles root-styles []
  {})


(defn root []
  (inject-global
    {:* {:box-sizing "border-box"}
     :a {:color "hsl(32, 91%, 54%)"
         :&:visited "hsl(32, 91%, 54%)"}
     :html
      {:font-family "Helvetica Neue, Helvetica, BlinkMacSystemFont, -apple-system, Roboto, 'Segoe UI', sans-serif"
       :font-weight 300}
     "h1,h2,h3" {:font-weight 300}})
  [:div#root.root {:class (root-styles)}
   [header/root]
   [q/query-ui]])
