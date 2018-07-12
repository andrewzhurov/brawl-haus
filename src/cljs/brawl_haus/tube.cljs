(ns brawl-haus.tube
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [pneumatic-tubes.core :as tubes]
            [brawl-haus.utils :refer [<sub l]]))

(defn on-receive [event-v]
  ;; handler of incoming events from server
  (.log js/console "<=evt:" (str event-v))
  (rf/dispatch event-v))

(def send-to-server (after (fn [db evt]
                             (when-let [tube (:tube db)]
                               (tubes/dispatch tube evt)))))

(reg-event-db
 :tube/connect
 (fn [db _]
   (let [tube (tubes/tube (str "ws://localhost:9090/tube") on-receive)]
     (tubes/create! tube)
     (assoc db :tube tube))))

(rf/reg-sub
 :tube
 (fn [db _]
   (get-in db [:init-opts :tube/id])))

(rf/reg-event-db
 :tube/send
 (fn [db [_ evt]]
   (when-let [tube (:tube db)]
     (tubes/dispatch tube (l "=>evt:" evt)))
   db))
