(ns brawl-haus.events
  (:require
   [re-frame.core :as rf]
   [brawl-haus.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   ))

(rf/reg-event-db
 :initialize-db
 (fn-traced [_ _]
   db/default-db))

(rf/reg-fx
 :play-sound
 (fn [_ [_ path]]
   (.play (js/Audio. path))))

(rf/reg-event-db
 :notification/remove
 (fn [db [_ n]]
   (update db :notifications disj n)))

(rf/reg-event-db
 :notification/create
 (fn [db [_ {:keys [duration] :as n}]]
   (js/setTimeout #(rf/dispatch [:notification/remove n]) (or duration 3000))
   (update db :notifications conj n)))

(rf/reg-sub
 :notification
 (fn [db _]
   (:notifications db)))

(rf/reg-event-fx
 :unauthorized
 (fn [{:keys [db]} _]
   (when-not (:asked-to-login db)
     {:dispatch-n [[:current-route :login]
                   [:notification/create {:text "How about introduction first?" #_"Давайте сначала познакомимся"
                                          :type :alert-warning}]]
      :db (assoc db :asked-to-login true)})))

(rf/reg-event-fx
 :success-newcome
 (fn [_ _]
   {:dispatch-n [[:current-route]
                 [:notification/create {:text "Welcome to the Haus!"
                                        :type :alert-success}]]}))

(rf/reg-event-fx
 :success-login
 (fn [_ _]
   {:dispatch-n [[:current-route]
                 [:notification/create {:text "We've been waiting!"
                                        :type :alert-success}]]}))

(rf/reg-event-fx
 :failed-login
 (fn [_ _]
   {:dispatch-n [[:notification/create {:text "Houston, we've had a problem here"
                                        :type :alert-warning}]]}))
