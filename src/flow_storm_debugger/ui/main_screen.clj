(ns flow-storm-debugger.ui.main-screen
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]))

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

(defn root [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :children [{:fx/type :label
                              :on-mouse-clicked {:event/type :clickeddddd
                                                 :some-data [42 42]}
                              :text (fx/sub-ctx context all-sub)}]}}})

(def renderer
  (fx/create-renderer
    :middleware (comp
                  ;; Pass context to every lifecycle as part of option map
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_] {:fx/type root})))
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

  (swap! *state fx/swap-context update :counter inc)
  (swap! *state fx/swap-context update :tasks conj {:name "AAA"})
  )
