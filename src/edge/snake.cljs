(ns edge.snake
  (:require
   [reagent.core :as r]))

(defonce app-state
  (r/atom {:version 0
           :offset 0
           :keys #{}}))

(defn translate-key [code]
  (case code
    65 :left
    68 :right
    87 :up
    83 :down
    code))

(defn snake []
  (fn []
    (let [state @app-state
          offset (:offset state)]
      [:g

       ;; White playing area with border
       [:rect {:width 300 :height 300 :style {:fill "white" :stroke-width 3 :stroke "black"}}]

       [:text {:x 10 :y 10 :fill "black" :font-family "monospace" :font-size 8}
        (str "Keys:" (pr-str (map translate-key (:keys state))))]

       [:rect {:x (mod (+ offset 100) 300) :y 100 :width 9 :height 9 :style {:fill "red"}}]
       [:rect {:x (mod (+ offset 110) 300) :y 100 :width 9 :height 9 :style {:fill "red"}}]
       [:rect {:x (mod (+ offset 120) 300) :y 100 :width 9 :height 9 :style {:fill "red"}}]
       [:rect {:x (mod (+ offset 130) 300) :y 100 :width 9 :height 9 :style {:fill "blue"}}]])))

(defn next-frame [state]
  (update state :offset + 10))

(defn frame [v]
  (when (= (get @app-state :version) v)
    (swap! app-state next-frame)
    (js/setTimeout (fn [] (frame v)) 500)))

(defn init [el]
  (println "init!")
  (println "?")
  (swap! app-state update :version inc)
  (swap! app-state assoc :offset 0)
  (r/render-component [snake] el)
  (set! js/window.onkeyup (fn [e] (swap! app-state update :keys disj (.-keyCode e))))
  (set! js/window.onkeydown (fn [e] (swap! app-state update :keys conj (.-keyCode e))))
  (js/setTimeout (fn [] (frame (get @app-state :version))) 0))
