(ns {{root-ns}}.frontend.main)

(js/console.log "Hello, world")

(set! (.-innerText (js/document.getElementById "app")) "Loaded {{name}}!")
