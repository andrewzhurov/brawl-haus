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
   [brawl-haus.panels.home]
   [brawl-haus.panels.ccc]
   [brawl-haus.components :as comps]
   [brawl-haus.shortcuts :as shortcuts]
   [brawl-haus.focus :as focus]))

(defn main-panel []
  (js/setTimeout #(rf/dispatch-sync [:conn/send [:view-data/subscribe :location]]) 2000)
  (fn []
    (let [conn-id (<sub [:db/get-in [:conn-id]])
          location (l "LOCATION: " (<sub [:view-data :location]))]
      [:div.main-layout {:key (str location)}
       [:nav.nav-section
        [:div.nav-wrapper
         [:a.brand-logo {:on-click #(rf/dispatch [:conn/send [:home/attend]])} "BrawlHaus"]
         [:ul#nav-mobile.right
          [:li.controls [:a {:on-click #(rf/dispatch [:help/toggle])}
                         [:i.material-icons "all_out"]]]]]]

       [:div.content-section
        [panels/panel location]]
       [comps/chat]
       [shortcuts/help]])))
