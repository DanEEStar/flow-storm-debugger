(ns flow-storm-debugger.ui.main-screen
  (:require [cljfx.api :as fx]
            [cljfx.prop :as fx.prop]
            [cljfx.mutator :as fx.mutator]
            [cljfx.lifecycle :as fx.lifecycle]
            [flow-storm-debugger.highlighter :as highlighter]
            [flow-storm-debugger.ui.db :as ui.db]
            [flow-storm-debugger.ui.subs :as ui.subs]
            [flow-storm-debugger.ui.events :as ui.events])
   (:import [javafx.scene.web WebView]))

#_(defn event-handler [ev]
  (println "GOT ev" ev)
  {:context (fx/swap-context (:fx/context ev) update :counter inc)})

#_(defn custom-fx [v dispatch!]
  (println "FX called with " v))

(def ext-with-html
  (fx/make-ext-with-props
    {:html (fx.prop/make
             (fx.mutator/setter #(.loadContent (.getEngine ^WebView %1) %2))
             fx.lifecycle/scalar)}))

(defn code-browser [{:keys [fx/context]}]
  (let [hl-forms (fx/sub-ctx context ui.subs/selected-flow-forms-highlighted)
        forms-html (->> hl-forms
                        (map (fn [[_ form-str]]
                               (str "<pre class=\"form\">" form-str "</pre>")))
                        (reduce str))]
   {:fx/type ext-with-html
    :props {:html (str "<div class=\"forms\">"
                       forms-html
                       "</div>")}
    :desc {:fx/type :web-view}}))

(defn bottom-bar [{:keys [fx/context]}]
  {:fx/type :pane
   :pref-height 50
   :style {:-fx-background-color :orange}
   :children []})

(defn controls-pane [{:keys [fx/context]}]
  (let [{:keys [traces trace-idx]} (fx/sub-ctx context ui.subs/selected-flow)]
   {:fx/type :border-pane
    ;;:pref-height 100
    :style {:-fx-background-color :pink
            :-fx-padding 10}
    :left {:fx/type :h-box
           :children [{:fx/type :button
                       :on-mouse-clicked {:event/type ::ui.events/set-current-flow-trace-idx :trace-idx 0}
                       :text "Reset"}
                      {:fx/type :button
                       :on-mouse-clicked {:event/type ::ui.events/selected-flow-prev}
                       :text "<"}
                      {:fx/type :button
                       :on-mouse-clicked {:event/type ::ui.events/selected-flow-next}
                       :text ">"}]}
    :center {:fx/type :label :text (str trace-idx "/" (count traces))}
    :right {:fx/type :h-box
            :children [{:fx/type :button :text "Load"}
                       {:fx/type :button :text "Save"}]}}))

(defn layers-pane [{:keys [fx/context]}]
  {:fx/type :pane
   :style {:-fx-background-color :pink}
   :children []})

(defn flow-tabs [{:keys [fx/context]}]
  {:fx/type :tab-pane
   :tabs [{:fx/type :tab
           :graphic {:fx/type :label :text "Flow1"}
           :content {:fx/type :border-pane
                     :style {:-fx-padding 10}
                     :top {:fx/type controls-pane}
                     :center {:fx/type :split-pane
                              :items [{:fx/type :tab-pane
                                       :tabs [{:fx/type :tab
                                               :graphic {:fx/type :label :text "Code"}
                                               :content {:fx/type code-browser}
                                               :id "code"
                                               :closable false}
                                              {:fx/type :tab
                                               :graphic {:fx/type :label :text "Layers"}
                                               :content {:fx/type layers-pane}
                                               :id "layers"
                                               :closable false}]}
                                      
                                      {:fx/type :split-pane
                                       :orientation :vertical
                                       :items [{:fx/type :pane
                                                :style {:-fx-background-color :blue}
                                                :children []}
                                               {:fx/type :pane
                                                :style {:-fx-background-color :yellow}
                                                :children []}]}]}}
           :id "tab1"
           :closable true}
          ]})

(defn main-screen [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :width 1000
   :height 1000
   :scene {:fx/type :scene
           :root {:fx/type :border-pane                  
                  :center {:fx/type flow-tabs}
                  :bottom {:fx/type bottom-bar}}}})

(defonce renderer
  (fx/create-renderer
    :middleware (comp
                  ;; Pass context to every lifecycle as part of option map
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_] {:fx/type main-screen})))
    :opts {:fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        ;; For functions in `:fx/type` values, pass
                                        ;; context from option map to these functions
                                        (fx/fn->lifecycle-with-context %))
           :fx.opt/map-event-handler (-> ui.events/dispatch-event
                                         (fx/wrap-co-effects
                                          {:fx/context (fx/make-deref-co-effect ui.db/*state)})
                                         (fx/wrap-effects
                                          {:context (fx/make-reset-effect ui.db/*state)
                                           :dispatch fx/dispatch-effect
                                           ;;:http custom-fx
                                           }))}))

(renderer)
(comment
  (fx/mount-renderer ui.db/*state renderer)
  

  (swap! *state fx/swap-context update :counter inc)
  (swap! *state fx/swap-context update :tasks conj {:name "AAA"})

  #_{:fx/type :grid-pane
     :column-constraints [{:fx/type :column-constraints :max-width 100}]
     :row-constraints [{:fx/type :row-constraints :max-height 100}]
     :children [{:fx/type :pane
                 :grid-pane/column 0
                 :grid-pane/row 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always                                       
                 :style {:-fx-background-color :red}
                 :children []}
                {:fx/type :pane
                 :grid-pane/column 1
                 :grid-pane/row 0
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :style {:-fx-background-color :green}
                 :children []}
                {:fx/type :pane
                 :grid-pane/column 1
                 :grid-pane/row 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :style {:-fx-background-color :blue}
                 :children []}
                {:fx/type :pane
                 :grid-pane/column 0
                 :grid-pane/row 1
                 :grid-pane/hgrow :always
                 :grid-pane/vgrow :always
                 :style {:-fx-background-color :yellow}
                 :children []}]}
  )
