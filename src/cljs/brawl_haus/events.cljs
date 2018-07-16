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
