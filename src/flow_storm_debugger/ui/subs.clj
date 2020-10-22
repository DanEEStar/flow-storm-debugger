(ns flow-storm-debugger.ui.subs
  (:require [cljfx.api :as fx]
            [flow-storm-debugger.highlighter :refer [highlight-expr]]
            [flow-storm-debugger.ui.utils :as utils]))

;; (defn counter-sub [context]
;;   (println "Firing counter-sub")
;;   (fx/sub-val context :counter))

;; (defn list-sub [context]
;;   (println "Firing list-sub")
;;   (fx/sub-val context :tasks))

;; (defn sorter-list-sub [context]
;;   (println "Firing sorter-list-sub")
;;   (sort-by :name (fx/sub-ctx context list-sub)))

;; (defn all-sub [context]
;;   (println "Firing all-sub")
;;   (str 
;;    (fx/sub-ctx context counter-sub)
;;    (fx/sub-ctx context sorter-list-sub)))

(defn flows [context]
  (fx/sub-val context :flows))

(defn selected-flow [context]
  (let [selected-flow-id (fx/sub-val context :selected-flow-id)
        flows (fx/sub-ctx context flows)]
    (-> (get flows selected-flow-id)
        (assoc :id selected-flow-id))))

(defn selected-flow-current-trace [context]
  (let [{:keys [traces trace-idx]} (fx/sub-ctx context selected-flow)]
    (get traces trace-idx)))

(defn selected-flow-forms [context]
  (let [selected-flow (fx/sub-ctx context selected-flow)]
    (:forms selected-flow)))

(defn selected-flow-forms-highlighted [context]
  (let [forms (fx/sub-ctx context selected-flow-forms)
        current-trace (fx/sub-ctx context selected-flow-current-trace)]
   (->> forms
        (mapv (fn [[form-id form-str]]
               [form-id
                (if (= form-id (:form-id current-trace))
                  (highlight-expr form-str (:coor current-trace) "<b class=\"hl\">" "</b>")
                  form-str)])))))

(defn flow-name [forms traces]
  (let [form-id (-> traces
                    first
                    :form-id)
        form (get forms form-id)
        str-len (count form)]
    (when form
     (cond-> form
       true (subs 0 (min 20 str-len))
       (> str-len 20) (str "...")))))

(defn flows-tabs [context]
  (let [flows (fx/sub-ctx context flows)]
    (->> flows
         (map (fn [[flow-id {:keys [forms traces]}]]
                [flow-id (flow-name forms traces)])))))

(defn selected-flow-pprint-panel-content [context]
  (:pprint-panel-content (fx/sub-ctx context selected-flow)))

(defn coor-in-scope? [scope-coor current-coor]
  (if (empty? scope-coor)
    true
    (every? true? (map = scope-coor current-coor))))

(defn trace-locals [{:keys [coor form-id timestamp]} bind-traces]
  (let [in-scope? (fn [bt]
                    (and (= form-id (:form-id bt))
                         (coor-in-scope? (:coor bt) coor)
                         (<= (:timestamp bt) timestamp)))]
    (when-not (empty? coor)
      (->> bind-traces          ;;(filter in-scope? bind-traces)
           (reduce (fn [r {:keys [symbol value] :as bt}]
                     (if (in-scope? bt)
                       (assoc r symbol value)
                       r))
                   {})))))

(defn selected-flow-bind-traces [context]
  (let [sel-flow (fx/sub-ctx context selected-flow)]
    (:bind-traces sel-flow)))

(defn selected-flow-current-locals [context]
  (let [{:keys [result] :as curr-trace} (fx/sub-ctx context selected-flow-current-trace)
        bind-traces (fx/sub-ctx context selected-flow-bind-traces)
        locals-map (trace-locals curr-trace bind-traces)]
    (->> locals-map
         (into [])
         (sort-by first)
         (into [["result" result true]]))))

(defn selected-flow-similar-traces [context]
  (let [{:keys [traces trace-idx]} (fx/sub-ctx context selected-flow)
        traces (mapv (fn [idx t] (assoc t :trace-idx idx :selected? (= idx trace-idx))) (range) traces)
        {:keys [form-id coor]} (get traces trace-idx)
        current-coor (get-in traces [trace-idx :coor])
        similar-traces (->> traces
                            (filter (fn similar [t]
                                      (and (= (:form-id t) form-id)
                                           (= (:coor t)    coor)
                                           (:result t)))))]
    similar-traces))

(comment
  (require '[flow-storm-debugger.ui.db :as ui.db])
  (selected-flow-forms-highlighted @ui.db/*state)
  )
