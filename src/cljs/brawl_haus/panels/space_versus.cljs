(ns brawl-haus.panels.space-versus
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub <=sub =>evt defview view]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]
            [brawl-haus.components :as comps]
            [paren-soup.core :as ps]
            [hiccups.runtime :as hiccups]
            [garden.core :as garden]
            #_[brawl-haus.events :as events]))

(defmethod panels/panel :space-versus
  [{{:keys [space-versus-id]} :params :as all}]
  (l 111 all)
  [:div.space-versus
   [:div.player.me
    [:img.ship-mock {:src "./image/my-ship.jpg"}]
    [:div.systems
     [:div.system.shields]
     [:div.system.engines]
     [:div.system.weapons]]
    [:div.bottom-hud
     [:div.energy-bar
      [:div.cell.with-power]
      [:div.cell.with-power]
      [:div.cell.without-power]
      [:div.cell.without-power]
      [:div.cell.without-power]
      [:div.cell.without-power]
      [:div.cell.without-power]]

     [:div.module
      [:div.icon.shields.with-power]
      [:div.cell.with-power]
      [:div.cell.with-power]
      [:div.cell.without-power]
      [:div.cell.without-power]]
     [:div.module
      [:div.icon.engines.with-power]
      [:div.cell.with-power]
      [:div.cell.with-power]
      [:div.cell.without-power]]
     [:div.module
      [:div.icon.weapons.with-power]
      [:div.cell.with-power]
      [:div.cell.without-power]]

     [:div.weapons-panel
      [:div.weapon.with-power
       [:div.readiness
        [:div.bar {:style {:height "99%"}}]]
       [:div.box
        [:div.power-require.with-power
         [:div.cell]
         [:div.cell]]
        [:div.name "Burst Laser II"]]]
      [:div.weapon.without-power
       [:div.readiness
        [:div.bar]]
       [:div.box
        [:div.power-require
         [:div.cell]]
        [:div.name "Basic Laser"]]]]]]
   [:div.player.enemy
    [:img.ship-mock {:src "./image/enemy-ship.png"}]]
   ])
