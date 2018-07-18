(ns brawl-haus.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [re-pressed.core :as rp]
   [brawl-haus.subs :as subs]
   [brawl-haus.utils :refer [l <sub]]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels.race]
   [brawl-haus.components :as comps]
   [brawl-haus.shortcuts :as shortcuts]))

(rf/reg-event-db
 :toggle-chat
 (fn [db _]
   (l "GOT IT" 11)
   (-> db
       (update :is-chat-open not)
       (assoc :is-help-open false))))

(defn main-panel []
  (let [tube (<sub [:tube])
        location (<sub [:db/get-in [:public-state :users (:tube/id tube) :location]])]
    [:div {:key (str location)}
     #_[panels/notifications]
     [panels/panel location]
     [comps/chat]
     [shortcuts/help-btn]
     [shortcuts/help]]))
