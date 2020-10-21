(ns flow-storm-debugger.server
  (:require [org.httpkit.server :as http-server]
            [compojure.core :as compojure :refer [GET POST]]
            [compojure.route :refer [resources]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.util.response :refer [resource-response]]
            [clojure.core.async :refer [go-loop] :as async]
            [ring.util.request :refer [body-string]]
            [flow-storm-debugger.ui.events :as ui.events]
            [flow-storm-debugger.ui.events.traces :as events.traces]
            [flow-storm-debugger.ui.db :as ui.db]
            [flow-storm-debugger.ui.main-screen :as ui.main-screen]
            [cljfx.api :as fx]))

(def server (atom nil))

(defn build-websocket []
  ;; TODO: move all this stuff to sierra components or something like that
  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket! (get-sch-adapter)
                                    {:csrf-token-fn nil
                                     :user-id-fn (fn [req] (:client-id req))})]

    {:ws-routes (compojure/routes (GET  "/chsk" req (ajax-get-or-ws-handshake-fn req))
                                  (POST "/chsk" req (ajax-post-fn                req)))
     :ws-send-fn send-fn
     :ch-recv ch-recv
     :connected-uids-atom connected-uids}))

(defn save-flow [{:keys [params] :as req}]
  (spit (str "./" (:file-name params)) (body-string req)))

(defn build-routes [opts]
  (compojure/routes
   (GET "/" [] (resource-response "index.html" {:root "public"}))
   (POST "/save-flow" req (do (save-flow req) {:status 200}) )
   (resources "/")))

#_(defn handle-ws-message [send-fn {:keys [event client-id user-id] :as msg}]
  (if (= client-id "browser")
    ;; message comming from the browser
    nil #_(println "browser -> tracer" event)

    ;; if client-id is not "browser" then it is one of the tracers
    ;; if we get a message from a tracer, just forward it to the browser
    (let [[evk] event]
      (if (#{:chsk/uidport-open :chsk/uidport-close} evk )
        ;; dec by one the count so we don't count the browser as another tracer
        (send-fn "browser" [:flow-storm/connected-clients-update {:count (dec (count (:any @(:connected-uids msg))))}])

        (when (#{:flow-storm/init-trace :flow-storm/add-trace :flow-storm/add-bind-trace} evk)
          (send-fn "browser" event))))))

(defn dispatch-local-event [event]
  (let [[e-key e-data-map] event]
    (case e-key
      :flow-storm/add-trace                (swap! ui.db/*state fx/swap-context events.traces/add-trace e-data-map) 
      :flow-storm/init-trace               (swap! ui.db/*state fx/swap-context events.traces/init-trace e-data-map)        
      :flow-storm/add-bind-trace           (swap! ui.db/*state fx/swap-context events.traces/add-bind-trace e-data-map) 
      :flow-storm/connected-clients-update (swap! ui.db/*state fx/swap-context assoc :connected-clients (:count e-data-map))
      (println "Donw know how to handle" event))
    (println "Got event " event)))

(defn -main [& args]
  (let [{:keys [ws-routes ws-send-fn ch-recv connected-uids-atom]} (build-websocket)
        port 7722
        dispatch-event dispatch-local-event]

    (go-loop []
      (try
        (let [event (:event (async/<! ch-recv))]
          (println "DISPATCHING " event)
         (dispatch-event event))
        (catch Exception e
          (println "ERROR handling ws message")))
      (recur))

    (fx/mount-renderer ui.db/*state ui.main-screen/renderer)
    (reset! server (http-server/run-server (-> (compojure/routes ws-routes (build-routes {:port port}))
                                              (wrap-cors :access-control-allow-origin [#"http://localhost:9500"]
                                                         :access-control-allow-methods [:get :put :post :delete])
                                              wrap-keyword-params
                                              wrap-params)
                                           {:port port}))
    #_(println "HTTP server running on 7722")))

(comment
  (def some-events [[:flow-storm/connected-clients-update {:count 1}]
                  [:flow-storm/init-trace {:flow-id 4094, :form-id 1195953040, :form-flow-id 94111, :form "(->> (range 3) (map inc))"}]
                  [:flow-storm/add-trace {:flow-id 4094, :form-id 1195953040, :form-flow-id 94111, :coor [2 1], :result "#function[clojure.core/inc]"}]
                  [:flow-storm/add-trace {:flow-id 4094, :form-id 1195953040, :form-flow-id 94111, :coor [1], :result "(0 1 2)"}]
                  [:flow-storm/add-trace {:flow-id 4094, :form-id 1195953040, :form-flow-id 94111, :coor [], :result "(1 2 3)"}]
                    [:flow-storm/add-trace {:flow-id 4094, :form-id 1195953040, :form-flow-id 94111, :coor [], :result "(1 2 3)", :outer-form? true}]])

  (def some-events-2 [[:flow-storm/connected-clients-update {:count 1}]
                      [:flow-storm/init-trace {:flow-id 333, :form-id 444, :form-flow-id 555, :form "(->> (range 4) (map inc))"}]
                      [:flow-storm/add-trace {:flow-id 333, :form-id 444, :form-flow-id 555, :coor [2 1], :result "#function[clojure.core/inc]"}]
                      [:flow-storm/add-trace {:flow-id 333, :form-id 444, :form-flow-id 555, :coor [1], :result "(0 1 2)"}]
                      [:flow-storm/add-trace {:flow-id 333, :form-id 444, :form-flow-id 555, :coor [], :result "(1 2 3)"}]
                      [:flow-storm/add-trace {:flow-id 333, :form-id 444, :form-flow-id 555, :coor [], :result "(1 2 3)", :outer-form? true}]])

  (doseq [e some-events-2]
    (dispatch-local-event nil [nil e]))
  
  (require '[flow-storm.api :as fsa])
  (def m-thread (Thread. (fn [] (-main))))
  (.start m-thread)
  (.stop m-thread)
  (fsa/connect)
 

  )
