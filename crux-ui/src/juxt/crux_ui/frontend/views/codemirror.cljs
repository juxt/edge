(ns juxt.crux-ui.frontend.views.codemirror
  (:require [reagent.core :as r]))


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
  [value-atom {:keys [style on-cm-init]}]

  (let [cm (atom nil)]
    (r/create-class

     {:component-did-mount
      (fn [this]
        (let [el   (r/dom-node this)
              opts #js {:lineNumbers false
                        :viewportMargin js/Infinity
                        :autofocus true
                        :value @value-atom
                        :theme "eclipse"
                        :autoCloseBrackets true
                        :matchBrackets true
                        :mode "clojure"}
              inst (js/CodeMirror. opts el)]
          (reset! cm inst)
          (.on inst "change"
               (fn []
                 (let [value (.getValue inst)]
                   (when-not (= value @value-atom)
                     (reset! value-atom value)))))
          (when on-cm-init
            (on-cm-init inst))))

      :component-did-update
      (fn [this old-argv]
        (when-not (= @value-atom (.getValue @cm))
          (.setValue @cm @value-atom)
          ;; reset the cursor to the end of the text, if the text was changed externally
          (let [last-line (.lastLine @cm)
                last-ch (count (.getLine @cm last-line))]
            (.setCursor @cm last-line last-ch))))

      :reagent-render
      (fn [_ _ _]
        @value-atom
        [:div {:style style}])})))
