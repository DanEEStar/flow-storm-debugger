(ns flow-storm-debugger.ui.subs.flows
  (:require [flow-storm-debugger.ui.utils :as utils]
            [flow-storm-debugger.highlighter :refer [highlight-expr]]))

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

(defn flows-tabs [db _]
  (->> (:flows db)
       (map (fn [[flow-id {:keys [forms traces]}]]
              [flow-id (flow-name forms traces)]))))

(defn selected-flow [{:keys [selected-flow-id] :as db} _]
   (-> db
       (get-in [:flows selected-flow-id])
       (assoc :id selected-flow-id)))

(defn selected-flow-result [{:keys [selected-flow-id] :as db} _]
  (let [{:keys [traces trace-idx]} (get-in db [:flows selected-flow-id])
        {:keys [result]} (get traces trace-idx)]
    result))

(defn selected-flow-forms [{:keys [forms] :as selected-flow} _]
  forms)

(defn selected-flow-traces [{:keys [traces] :as selected-flow} _]
  traces)

(defn selected-flow-trace-idx [{:keys [trace-idx]} _]
  trace-idx)

(defn selected-flow-similar-traces [[traces trace-idx] _]
   (let [traces (mapv (fn [idx t] (assoc t :trace-idx idx)) (range) traces)
         {:keys [form-id coor]} (get traces trace-idx)
         current-coor (get-in traces [trace-idx :coor])
         similar-traces (->> traces
                             (filter (fn similar [t]
                                       (and (= (:form-id t) form-id)
                                            (= (:coor t)    coor)))))]
     similar-traces))

(defn selected-flow-current-trace [{:keys [traces trace-idx]} _]
  (get traces trace-idx))

(defn selected-flow-forms-highlighted [[forms current-trace] _]
   (->> forms
        (map (fn [[form-id form-str]]
               [form-id
                (if (= form-id (:form-id current-trace))
                  (highlight-expr form-str (:coor current-trace) "<b class=\"hl\">" "</b>")
                  form-str)]))))

(defn selected-flow-bind-traces [{:keys [bind-traces]} _]
  bind-traces)

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

(defn selected-flow-current-locals [[curr-trace bind-traces]]
   (let [locals-map (trace-locals curr-trace bind-traces)]
     (->> locals-map
          (into [])
          (sort-by first))))

(defn selected-result-panel [{:keys [selected-result-panel] :as db} _]
  selected-result-panel)

(defn current-flow-local-panel [flow _]
  (:local-panel flow))

(defn form-name [form-str]
  (let [[_ form-name] (when form-str
                        (re-find #"\([\S]+\s([\S]+)\s.+" form-str))]
    form-name))

(defn fn-call-trace? [trace]
  (:args-vec trace))

(defn ret-trace? [trace]
  (and (:result trace)
       (:outer-form? trace)))

(defn build-tree-from-traces [traces]
  (loop [[t & r] (rest traces)
         tree (-> (first traces)
                  (assoc :childs []))
         path [:childs]]
    (let [last-child-path (into path [(count (get-in tree path))])]
      (cond
        (nil? t) tree
        (fn-call-trace? t) (recur r
                                  (update-in tree last-child-path #(merge % (assoc t :childs [])))
                                  (into last-child-path [:childs]))
        (ret-trace? t) (let [ret-pointer (vec (butlast path))]
                         (recur r
                                (if (empty? ret-pointer)
                                  (merge tree t)
                                  (update-in tree ret-pointer merge t ))
                                (vec (butlast (butlast path)))))))))

(defn fn-call-traces [[traces forms] _]
  (let [call-traces (->> traces
                         (map-indexed (fn [idx t]
                                        (if (fn-call-trace? t)
                                          (assoc t :call-trace-idx idx)
                                          (assoc t :ret-trace-idx idx))))
                         (filter (fn [t] (or (fn-call-trace? t)
                                             (ret-trace? t)))))]
    (when (some #(:fn-name %) call-traces)
      (build-tree-from-traces call-traces))))

(defn save-flow-panel-open? [{:keys [save-flow-panel-open?]} _]
   save-flow-panel-open?)

(defn save-flow-panel-open? [flow _]
  (:save-flow-panel-open? flow))
