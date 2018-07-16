(ns brawl-haus.subs
  (:require
   [re-frame.core :as re-frame]
   [brawl-haus.utils :refer [l <sub]]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(re-frame/reg-sub
 :user
 :<- [:tube]
 :<- [:users]
 (fn [[tube users] [_ tube-id]]
   (get users (or tube-id (:tube/id tube)))))
