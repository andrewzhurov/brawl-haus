(ns brawl-haus.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::re-pressed-example
 (fn [db _]
   (:re-pressed-example db)))

(re-frame/reg-sub
 :user
 (fn [db _]
   (get-in db [:private-state :user])))

(re-frame/reg-sub
 :nick
 (fn [db _]
   (get-in db [:private-state :user :nick])))
