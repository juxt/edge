(ns juxt.crux-ui.frontend.views.codemirror
  (:require [reagent.core :as r]
            [cljss.core]
            [garden.core :as garden]
            ["/codemirror/lib/codemirror.js" :as codemirror]
            ["/codemirror/mode/clojure/clojure.js"]
            ["/codemirror/addon/edit/closebrackets.js"]
            ["/codemirror/addon/edit/matchbrackets.js"]))


(def code-mirror-styling
  (garden/css
    [[:.code-mirror-container
       {:font-size :17px}]
     [:.CodeMirror
      {:border-radius :2px
       :padding "8px 8px"}]]))

(defn code-mirror
  [initial-value {:keys [on-change on-cm-init]}]

  (let [value-atom (atom (or initial-value ""))
        on-change  (or on-change (constantly nil))
        cm-inst    (atom nil)]
    (r/create-class

     {:component-did-mount
      (fn [this]
        (let [el   (r/dom-node this)
              opts #js {:lineNumbers false
                        :viewportMargin js/Infinity
                        :autofocus true
                        :value @value-atom
                        :theme "monokai"
                        :autoCloseBrackets true
                        :matchBrackets true
                        :mode "clojure"}
              inst (codemirror. el opts)]
          (reset! cm-inst inst)
          (.on inst "change"
               (fn []
                 (let [value (.getValue inst)]
                   (when-not (= value @value-atom)
                     (on-change value)
                     (reset! value-atom value)))))
          (when on-cm-init
            (on-cm-init inst))))

      :component-did-update
      (fn [this old-argv]
        (when-not (= @value-atom (.getValue @cm-inst))
          (.setValue @cm-inst @value-atom)
          ;; reset the cursor to the end of the text, if the text was changed externally
          (let [last-line (.lastLine @cm-inst)
                last-ch (count (.getLine @cm-inst last-line))]
            (.setCursor @cm-inst last-line last-ch))))

      :reagent-render
      (fn [_ _ _]
        [:div.code-mirror-container
         [:style code-mirror-styling]])})))
