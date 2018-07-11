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
   [brawl-haus.panels.home]
   [brawl-haus.panels.races]
   [brawl-haus.panels.race]
   [brawl-haus.panels.login]))



(defn main-panel []
  (let [{:keys [handler route-params]} (<sub [:db/get-in [:current-panel]])
        user (<sub [:user])]
    (when (nil? user) (rf/dispatch [:unauthorized]))
    [:div
     [panels/notifications]
     [panels/panel handler route-params]]))
