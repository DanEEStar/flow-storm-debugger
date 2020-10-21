(ns flow-storm-debugger.ui.subs
  (:require [re-frame.core :refer [reg-sub]]                      
            [flow-storm-debugger.ui.subs.flows :as subs.flows]
            [flow-storm-debugger.ui.subs.general :as subs.general]))

(reg-sub ::flows-tabs subs.flows/flows-tabs)

(reg-sub ::selected-flow subs.flows/selected-flow)

(reg-sub
 ::selected-flow-result
 subs.flows/selected-flow-result)

(reg-sub
 ::selected-flow-forms
 :<- [::selected-flow]
 subs.flows/selected-flow-forms)

(reg-sub
 ::selected-flow-traces
 :<- [::selected-flow]
 subs.flows/selected-flow-traces)

(reg-sub
 ::selected-flow-trace-idx
 :<- [::selected-flow]
 subs.flows/selected-flow-trace-idx)

(reg-sub
 ::selected-flow-similar-traces
 :<- [::selected-flow-traces]
 :<- [::selected-flow-trace-idx]
 subs.flows/selected-flow-similar-traces)

(reg-sub
 ::selected-flow-current-trace
 :<- [::selected-flow]
 subs.flows/selected-flow-current-trace)

(reg-sub
 ::selected-flow-forms-highlighted
 :<- [::selected-flow-forms]
 :<- [::selected-flow-current-trace]
 subs.flows/selected-flow-forms-highlighted)

(reg-sub
 ::selected-flow-bind-traces
 :<- [::selected-flow]
 subs.flows/selected-flow-bind-traces)

(reg-sub
 ::selected-flow-current-locals
 :<- [::selected-flow-current-trace]
 :<- [::selected-flow-bind-traces]
 subs.flows/selected-flow-current-locals)

(reg-sub
 ::selected-result-panel
 subs.flows/selected-result-panel)

(reg-sub
 ::current-flow-local-panel
 :<- [::selected-flow]
 subs.flows/current-flow-local-panel)

(reg-sub
 ::fn-call-traces
 :<- [::selected-flow-traces]
 :<- [::selected-flow-forms]
 subs.flows/fn-call-traces)

(reg-sub
 ::save-flow-panel-open?
 subs.flows/save-flow-panel-open?)

(reg-sub
 ::save-flow-panel-open?
 :<- [::selected-flow]
 subs.flows/save-flow-panel-open?)

(reg-sub
 ::connected-clients
 subs.general/connected-clients)
