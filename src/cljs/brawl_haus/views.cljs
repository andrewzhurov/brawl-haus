(ns brawl-haus.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [re-pressed.core :as rp]
   [brawl-haus.subs :as subs]
   [brawl-haus.utils :refer [l <sub]]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels.stand-by]
   [brawl-haus.panels.home]
   [brawl-haus.panels.races]
   [brawl-haus.panels.race]
   [brawl-haus.panels.login]))



(defn main-panel []
  (let [user (l "User:" (<sub [:user]))
        location (l "Location:" (<sub [:db/get-in [:public-state :user-locations (:nick user)]]))]
    (cond (nil? user)
          (do (rf/dispatch [:tube/send [:login/anonymous]])
              [:div "Waiting to be logged in..."])

          :logged-in
          [:div
           [panels/notifications]
           [panels/panel location]])))
