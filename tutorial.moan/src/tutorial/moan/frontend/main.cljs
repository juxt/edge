(ns tutorial.moan.frontend.main)

(js/console.log "Hello, world")

(set! (.-innerText (js/document.getElementById "app")) "Loaded moan!")
