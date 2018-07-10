(ns brawl-haus.ws
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [pneumatic-tubes.core :as tubes]))

(defn on-receive [event-v]
  ;; handler of incoming events from server
  (.log js/console "received from server:" (str event-v))
  (rf/dispatch event-v))


;; definition of event 'tube' over WebSocket
(def send-to-server (after (fn [db evt]
                             (rf/dispatch [:tube-send evt]))))
(rf/reg-fx
 :tube-send
 (fn [db [_ evt]]
   (when-let [tube (:tube db)]
     (tubes/dispatch tube evt))))
;; middleware to send event to server

(reg-event-db ;; normal re-frame handler
 :add-message
 send-to-server ;; forwards this event also to server
 (fn [db [_ msg]]
   (.log js/console (str "Trying to add msg: " msg))
   db))

(reg-event-db ;; will be called by server
 :added-message
 (fn [db [_ msg]]
   (update db :messages conj msg)))

(reg-sub
 :db/get-in
 (fn [db [_ path]]
   (get-in db path)))

(reg-event-db
 :db/set-in
 (fn [db [_ path val]]
   (assoc-in db path val)))

(reg-event-db
 :connect-tube
 (fn [db _]
   (let [tube (tubes/tube (str "ws://localhost:9090/ws") on-receive)]
     (tubes/create! tube)
     (assoc db :tube tube))))
