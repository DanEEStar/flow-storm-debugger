(ns flow-storm-debugger.ui.main-screen
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [cljfx.prop :as fx.prop]
            [cljfx.mutator :as fx.mutator]
            [cljfx.lifecycle :as fx.lifecycle]
            [flow-storm-debugger.highlighter :as highlighter])
   (:import [javafx.scene.web WebView]))

(def *state
  (atom (fx/create-context {:counter 0
                            :tasks [{:name "test"}
                                    {:name "juan"}
                                    {:name "bla"}
                                    {:name "xxx"}]}
                           cache/lru-cache-factory)))

(defn counter-sub [context]
  (println "Firing counter-sub")
  (fx/sub-val context :counter))

(defn list-sub [context]
  (println "Firing list-sub")
  (fx/sub-val context :tasks))

(defn sorter-list-sub [context]
  (println "Firing sorter-list-sub")
  (sort-by :name (fx/sub-ctx context list-sub)))

(defn all-sub [context]
  (println "Firing all-sub")
  (str 
   (fx/sub-ctx context counter-sub)
   (fx/sub-ctx context sorter-list-sub)))

(defn event-handler [ev]
  (println "GOT ev" ev)
  {:context (fx/swap-context (:fx/context ev) update :counter inc)})

(defn custom-fx [v dispatch!]
  (println "FX called with " v))

(def ext-with-html
  (fx/make-ext-with-props
    {:html (fx.prop/make
             (fx.mutator/setter #(.loadContent (.getEngine ^WebView %1) %2))
             fx.lifecycle/scalar)}))

(defn code-browser []
  {:fx/type ext-with-html
   :props {:html (str "<pre>"
                      (highlighter/highlight-expr "(->> (range 10)\n     (map inc)\n     (filter odd?) \n     (reduce +))" [3] "<b>" "</b>")
                      "</pre>")}
   :desc {:fx/type :web-view}})

(defn bottom-bar [{:keys []}]
  {:fx/type :pane
   :pref-height 50
   :style {:-fx-background-color :orange}
   :children []})

(defn main-screen [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :width 1000
   :height 1000
   :scene {:fx/type :scene
           :root {:fx/type :border-pane                  
                  :center {:fx/type :tab-pane
                           :tabs [{:fx/type :tab
                                   :graphic {:fx/type :label
                                             :text "Flow1"}
                                   :content {:fx/type :split-pane
                                             :items [{:fx/type :split-pane
                                                      :orientation :vertical
                                                      :items [{:fx/type :pane
                                                               :style {:-fx-background-color :red}
                                                               :children []}
                                                              {:fx/type :pane
                                                               :style {:-fx-background-color :green}
                                                               :children []}]}
                                                     {:fx/type :split-pane
                                                      :orientation :vertical
                                                      :items [{:fx/type :pane
                                                               :style {:-fx-background-color :blue}
                                                               :children []}
                                                              {:fx/type :pane
                                                               :style {:-fx-background-color :yellow}
                                                               :children []}]}]}
                                   :id "tab1"
                                   :closable true}
                                  {:fx/type :tab
                                   :graphic {:fx/type :label
                                             :text "Flow2"}
                                   :content {:fx/type :split-pane
                                             :items [{:fx/type :split-pane
                                                      :orientation :vertical
                                                      :items [{:fx/type :pane
                                                               :style {:-fx-background-color :green}
                                                               :children []}
                                                              {:fx/type :pane
                                                               :style {:-fx-background-color :green}
                                                               :children []}]}
                                                     {:fx/type :split-pane
                                                      :orientation :vertical
                                                      :items [{:fx/type :pane
                                                               :style {:-fx-background-color :green}
                                                               :children []}
                                                              {:fx/type :pane
                                                               :style {:-fx-background-color :green}
                                                               :children []}]}]}
                                   :id "tab2"
                                   :closable true}]}
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
                  :bottom {:fx/type bottom-bar}}}})

(def renderer
  (fx/create-renderer
    :middleware (comp
                  ;; Pass context to every lifecycle as part of option map
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_] {:fx/type main-screen})))
    :opts {:fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        ;; For functions in `:fx/type` values, pass
                                        ;; context from option map to these functions
                                        (fx/fn->lifecycle-with-context %))
           :fx.opt/map-event-handler (-> event-handler
                                         (fx/wrap-co-effects
                                          {:fx/context (fx/make-deref-co-effect *state)})
                                         (fx/wrap-effects
                                          {:context (fx/make-reset-effect *state)
                                           :dispatch fx/dispatch-effect
                                           :http custom-fx}))}))


(comment
  (fx/mount-renderer *state renderer)
(renderer)
  (swap! *state fx/swap-context update :counter inc)
  (swap! *state fx/swap-context update :tasks conj {:name "AAA"})
  )
