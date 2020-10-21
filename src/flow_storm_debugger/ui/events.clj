(ns flow-storm-debugger.ui.events
  (:require [flow-storm-debugger.ui.events.flows :as events.flows]
            [flow-storm-debugger.ui.events.traces :as events.traces]
            [cljfx.api :as fx]))

(defmulti dispatch-event :event/type)

(defmethod dispatch-event ::init [{:keys [fx/context]}]
  {:context context})

(defmethod dispatch-event ::selected-flow-prev [{:keys [fx/context]}]
  {:context (fx/swap-context context events.flows/selected-flow-prev)})

(defmethod dispatch-event ::selected-flow-next [{:keys [fx/context]}]
  {:context (fx/swap-context context events.flows/selected-flow-next)})

(defmethod dispatch-event ::select-flow [{:keys [fx/context flow-id]}]
  {:context (fx/swap-context context events.flows/select-flow flow-id)})

(defmethod dispatch-event ::remove-flow [{:keys [fx/context flow-id]}]
  {:context (fx/swap-context context events.flows/remove-flow flow-id)})

(defmethod dispatch-event ::set-current-flow-trace-idx [{:keys [fx/context trace-idx]}]
  {:context (fx/swap-context context events.flows/set-current-flow-trace-idx trace-idx)})

;; (defmethod dispatch-event ::save-selected-flow [{:keys [fx/context]}]
;;   ;;{:context (fx/swap-context context )}
;;   )
;; (defmethod dispatch-event ::load-flow [{:keys [fx/context]}]
;;   ;;{:context (fx/swap-context context )}
;;   )

;; (defmethod dispatch-event ::select-result-panel [{:keys [fx/context]}]
;;   {:context (fx/swap-context context )}
;;   )
;; (defmethod dispatch-event ::show-local [{:keys [fx/context]}]
;;   {:context (fx/swap-context context )}
;;   )
;; (defmethod dispatch-event ::hide-modals [{:keys [fx/context]}]
;;   {:context (fx/swap-context context )}
;;   )
;; (defmethod dispatch-event ::open-save-panel [{:keys [fx/context]}]
;;   {:context (fx/swap-context context )}
;;   )
