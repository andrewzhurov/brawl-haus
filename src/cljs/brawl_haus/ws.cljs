(ns brawl-haus.ws
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [brawl-haus.tube :as tube]))

(defn l [desc expr] (js/console.log desc expr) expr)

; Race
(reg-event-db ;; normal re-frame handler
 :new-race
 tube/send-to-server ;; forwards this event also to server
 (fn [db _]
   (.log js/console (str "Initiating a new race"))
   db))

(reg-event-db
 :race-initiated
 (fn [db [_ race]]
   (.log js/console (str "Race initiated:" race))
   (rf/dispatch [:current-route :race (:id race)])
   db))

(rf/reg-event-fx
 :enter-race
 (fn [{:keys [db]} [_ race-id]]
   {:dispatch-n [[:tube/send [:enter-race race-id]]
                 [:current-route :race race-id]]}))

(reg-event-db
 :current-public-state
 (fn [db [_ public-state]]
   (l "DB:" db)
   (assoc db :public-state public-state)))

(reg-event-db
 :init
 (fn [db [_ init-opts]]
   (assoc db :init-opts init-opts)))

#_(reg-event-db
 :open-races
 (fn [db [_ open-races]]
   (.log js/console (str "Open races:" open-races))
   (assoc db :open-races open-races)))

#_(reg-event-db ;; normal re-frame handler
 :connect-to-race
 tube/send-to-server ;; forwards this event also to server
 (fn [db _]
   (.log js/console (str "Initiating a new race"))
   db))
