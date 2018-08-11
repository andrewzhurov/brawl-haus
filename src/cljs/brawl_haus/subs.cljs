(ns brawl-haus.subs
  (:require
   [re-frame.core :as rf]
   [brawl-haus.utils :refer [l <sub]]))

(rf/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(rf/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(rf/reg-sub
 :conn-id
 (fn [db _]
   (:conn-id db)))

(rf/reg-sub
 :user
 :<- [:conn-id]
 :<- [:users]
 (fn [[my-conn-id users] [_ conn-id]]
   (get users (or my-conn-id conn-id))))
