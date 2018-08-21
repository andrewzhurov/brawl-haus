(ns brawl-haus.utils
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :dirt
 (fn [_ [_ effect-map]]
   effect-map))

(rf/reg-sub
 :db/get-in
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-event-db
 :db/set-in
 (fn [db [_ path val]]
   (assoc-in db path val)))

(defn <sub [evt] (deref (rf/subscribe evt)))
(defn >evt [evt] (rf/dispatch evt))

(defn =>evt [evt] (rf/dispatch [:conn/send evt]))

(defn l [desc expr] (js/console.log desc expr) expr)

(def views (atom {}))
(defn defview [view-id render-fn]
  (swap! views assoc view-id render-fn))

(defn view [view-id]
  (rf/dispatch [:conn/send [:view-data/subscribe view-id]])
  (fn []
    ((get @views view-id) (<sub [:view-data view-id]))))

(rf/reg-event-db
 :view-data
 (fn [db [_ view-id view-data]]
   (assoc-in db [:view-data/subs view-id] view-data)))

(rf/reg-sub
 :view-data
 (fn [db [_ view-id]]
   (get-in db [:view-data/subs view-id])))
