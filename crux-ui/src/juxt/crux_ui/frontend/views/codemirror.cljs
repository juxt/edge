(ns juxt.crux-ui.frontend.views.codemirror
  (:require [reagent.core :as r]
            ["/codemirror/lib/codemirror.js" :as codemirror]
            ["/codemirror/mode/clojure/clojure.js"]
          ; ["/codemirror/keymap/emacs.inc.js"]
            ["/codemirror/addon/edit/closebrackets.js"]
            ["/codemirror/addon/edit/matchbrackets.js"]
           ;[codemirror.mode.clojure]
            ))


(defn code-mirror
  "Create a code-mirror editor. The parameters:
  value-atom (reagent atom)
    when this changes, the editor will update to reflect it.
  options
  :style (reagent style map)
    will be applied to the container element
  :js-cm-opts
    options passed into the CodeMirror constructor
  :on-cm-init (fn [cm] -> nil)
    called with the CodeMirror instance, for whatever extra fiddling you want to do."
  [initial-value {:keys [style on-cm-init]}]

  (let [value-atom (atom (or initial-value ""))
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
        [:div.code-mirror-container {:style style}])})))
