(ns hn.event
  (:require [jsonista.core :as json])
  (:import [java.awt Desktop]
           [java.net URI]))

(def ^:private mapper
  (json/object-mapper {:decode-key-fn keyword}))

(defn- parse-response-body [response]
  (json/read-value (:body response) mapper))

(defmulti handle ::type)

(defmethod handle ::load-stories [_]
  {:http {:method :get
          :url "https://hacker-news.firebaseio.com/v0/topstories.json"
          :on-response {::type ::process-stories}}})

(defmethod handle ::process-stories [{:keys [state response]}]
  (let [stories (parse-response-body response)]
    (into [[:state (assoc state :stories stories)]]
          (map (fn [id]
                 [:dispatch {::type ::load-story :id id}]))
          stories)))

(defmethod handle ::load-story [{:keys [id]}]
  {:http {:method :get
          :url (str "https://hacker-news.firebaseio.com/v0/item/" id ".json")
          :on-response {::type ::process-story :id id}}})

(defmethod handle ::process-story [{:keys [state id response]}]
  {:state (assoc-in state [:items id] (parse-response-body response))})

(defmethod handle ::open-story [{:keys [state id]}]
  (let [comment-ids (get-in state [:items id :kids])]
    (into [[:state (assoc state :view [:story id])]]
          (map (fn [id]
                 [:dispatch {::type ::load-comment :id id}]))
          comment-ids)))

(defmethod handle ::load-comment [{:keys [id]}]
  {:http {:method :get
          :url (str "https://hacker-news.firebaseio.com/v0/item/" id ".json")
          :on-response {::type ::process-comment :id id}}})

(defmethod handle ::process-comment [{:keys [state id response]}]
  (let [comment (parse-response-body response)]
    (into [[:state (assoc-in state [:items id] comment)]]
          (map (fn [id]
                 [:dispatch {::type ::load-comment :id id}]))
          (:kids comment))))

(defmethod handle ::open-stories [{:keys [state]}]
  {:state (assoc state :view [:stories])})

(defmethod handle ::open-url [{:keys [url]}]
  (future
    (.browse (Desktop/getDesktop) (URI. url)))
  nil)