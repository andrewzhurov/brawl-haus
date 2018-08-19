(ns brawl-haus.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [re-pressed.core :as rp]
   [brawl-haus.utils :refer [l <sub]]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels.race]
   [brawl-haus.panels.hiccup-touch]
   [brawl-haus.components :as comps]
   [brawl-haus.shortcuts :as shortcuts]
   [brawl-haus.focus :as focus]))

(rf/reg-event-db
 :toggle-chat
 (fn [db _]
   (if (:is-chat-open db)
     (focus/focus-previous)
     (.. js/document (getElementById "chat-input") focus))
   (-> db
       (update :is-chat-open not)
       (assoc :is-help-open false))))

(defn main-panel []
  (js/setTimeout #(rf/dispatch-sync [:conn/send [:view-data/subscribe :location]]) 2000)
  (fn []
    (let [conn-id (<sub [:db/get-in [:conn-id]])
          location (l "LOCATION: " (<sub [:view-data :location]))]
      [:div {:key (str location)}
       #_[panels/notifications]
       [panels/panel location]
       [comps/chat]
       [shortcuts/help-btn]
       [:div.btn.hiccup-touch {:on-click #(rf/dispatch [:conn/send [:hiccup-touch/attend]])} "Hiccup practice"]
       [shortcuts/help]])))
