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

(defn l [desc expr] (js/console.log desc expr) expr)
