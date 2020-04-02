(ns hn.core
  (:require [cljfx.api :as fx]
            [clj-http.client :as http]
            [hn.event :as event]
            [hn.view :as view])
  (:import [javafx.application Platform]
           [java.util.concurrent Executors ThreadFactory])
  (:gen-class))

(defn http-effect [v dispatch!]
  (try
    (http/request
      (-> v
          (assoc :async true :as :stream)
          (dissoc :on-response :on-exception))
      (fn [response]
        (dispatch! (assoc (:on-response v) :response response)))
      (fn [exception]
        (dispatch! (assoc (:on-exception v) :exception exception))))
    (catch Exception e
      (dispatch! (assoc (:on-exception v) :exception e)))))

(def daemon-executor
  (let [*counter (atom 0)
        factory (reify ThreadFactory
                  (newThread [_ runnable]
                    (doto (Thread. runnable (str "reveal-agent-pool-" (swap! *counter inc)))
                      (.setDaemon true))))]
    (Executors/newCachedThreadPool factory)))

(def *state
  (atom {:view [:stories]}))

(def event-handler
  (-> event/handle
      (fx/wrap-co-effects
        {:state #(deref *state)})
      (fx/wrap-effects
        {:dispatch fx/dispatch-effect
         :state (fx/make-reset-effect *state)
         :http http-effect})
      (fx/wrap-async :fx/executor daemon-executor)))

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc #'view/app)
    :opts {:fx.opt/map-event-handler event-handler}))

(defn -main []
  (Platform/setImplicitExit true)
  (fx/mount-renderer *state renderer)
  (event-handler {::event/type ::event/load-stories}))