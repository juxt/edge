(ns ^:figwheel-hooks {{root-ns}}.frontend.main)

(js/console.log "Hello, world")

;; This is called once
(defonce init
  (do (set! (.-innerHTML (js/document.getElementById "app"))
            "<p>Loaded {{name}}!</p>
            <p>Edit src/{{sanitized}}/frontend/main.cljs to change this message.</p>")
      true))

;; This is called every time you make a code change
(defn ^:after-load reload []
  (set! (.-innerText (js/document.getElementById "app")) "Reloaded {{name}}!"))
