(ns brawl-haus.tube
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [pneumatic-tubes.core :as tubes]
            [brawl-haus.utils :refer [<sub l]]
            [config.core :refer [env]]))

(defn on-receive [event-v]
  ;; handler of incoming events from server
  (rf/dispatch (l "<=evt" event-v)))

(defn tube [db]
  (:tube db))

(rf/reg-sub
 :tube
 (fn [db _]
   (tube db)))

(rf/reg-sub
 :id
 :<- [:tube]
 (fn [[tube] _]
   (:tube/id tube)))


(reg-event-db
 :tube/create
 (fn [db _]
   (let [tube (tubes/tube (str "ws://" (:server-url env) "/tube") on-receive)]
     (tubes/create! tube)
     (assoc db :tube tube))))

;<=
(reg-event-db
 :tube/did-create
 (fn [db [_ tube-id]]
   (assoc-in db [:tube :tube/id] tube-id)))

(rf/reg-event-db
 :tube/send
 (fn [db [_ evt]]
   (when-let [tube (tube db)]
     (tubes/dispatch tube (l "=>evt" evt)))
   db))
