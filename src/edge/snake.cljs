(ns edge.snake
  (:require
   [reagent.core :as r]))

(defonce player-state
  (r/atom
   {:keys #{}}))

(defonce app-state
  (r/atom
   {:version 0
    :snake []}))

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

       #_(for [x (range 30)]
         [:g
          [:text {:x (+ 5 (* 10 x)) :y 7 :fill "black" :font-family "monospace" :font-size 4}
           x]
          [:line {:x1 (* 10 x) :x2 (* 10 x) :y1 0 :y2 300 :stroke "blue"}]]
         )

       #_(for [y (range 30)]
         [:g
          [:text {:x 5 :y (+ 7 (* 10 y)) :fill "black" :font-family "monospace" :font-size 4}
           y]
          [:line {:x1 0 :x2 300 :y1 (* 10 y) :y2 (* 10 y) :stroke "blue"}]
          ]
         )

       #_[:text {:x 10 :y 10 :fill "black" :font-family "monospace" :font-size 8}
        (str "Keys:" (pr-str (map translate-key (:keys state))))]


       [:g
        (for [[x y color] (map conj (:snake state) (concat ["black"] (repeat "red")))]
          [:rect {:x (* 10 x) :y (* 10 y) :width 10 :height 10 :style {:fill color :stroke-width 1 :stroke "black"}}]

          )]])))

(defn next-point [[x y] direction]
  (case direction
    :left [(dec x) y]
    :right [(inc x) y]
    :up [x (dec y)]
    :down [x (inc y)]
    [x y])
  )

(defn update-direction [state keys]
  (let [pressedkey (first keys)]
    (cond-> state
      pressedkey (assoc :direction (translate-key pressedkey)))))

(defn update-snake [state]
  (let [[x y] (first (:snake state))]
    (update state :snake (fn [snake] (cons (:next-head state) (take 100 snake))))))

(defn calc-head [state]
  (let [[x y] (first (:snake state))]
    (assoc state :next-head (next-point (first (:snake state))
                                        (:direction state)))))


(defn next-frame [state]
  (-> state
      (update-direction (:keys @player-state))
      (calc-head)
      (update-snake)
      ))

(defn frame [v]
  (when (and (= (get @app-state :version) v)
             (not (contains? @app-state :game-over)))
    (swap! app-state next-frame)
    (js/setTimeout (fn [] (frame v)) 200)))

(defn init [el]
  (println "init!")
  (println "?")
  (swap! app-state update :version inc)
  (swap! app-state assoc :snake [[10 10] [9 10] [8 10] [7 10] [6 10]]
         :direction :right)
  (r/render-component [snake] el)
  (set! js/window.onkeyup (fn [e] (swap! player-state update :keys disj (.-keyCode e))))
  (set! js/window.onkeydown (fn [e] (swap! player-state update :keys conj (.-keyCode e))))
  (js/setTimeout (fn [] (frame (get @app-state :version))) 0))
