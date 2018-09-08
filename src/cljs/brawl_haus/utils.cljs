(ns brawl-haus.utils
  (:require [reagent.ratom]
            [re-frame.core :as rf]))

(defn l [desc expr] (js/console.log desc expr) expr)

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
(defn <=sub [sub]
  (when (not (<sub [:subscribed? sub]))
    (=>evt [:subscribe sub]))
  (deref (reagent.ratom/make-reaction
          (fn [] (<sub [:derived-data sub]))
          ;:on-dispose #(=>evt [:unsubscribe sub])
          )))


(def views (atom {}))
(defn defview [view-id render-fn]
  (swap! views assoc view-id render-fn))

(defn view [view-id]
  (rf/dispatch [:conn/send [:subscribe [view-id]]])
  (fn []
    ((get @views view-id) (<sub [:derived-data [view-id]]))))

(rf/reg-sub
 :subscribed?
 (fn [db [_ sub]]
   (contains? (set (get-in db [:derived-data [:my-subs]])) sub)))

(rf/reg-event-db
 :derived-data
 (fn [db [_ sub data]]
   (assoc-in db [:derived-data sub] data)))

(rf/reg-sub
 :derived-data
 (fn [db [_ sub]]
   (get-in db [:derived-data sub])))
