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

(defn weapons []
  [:div.weapons-panel
   (doall
    (for [{:keys [stuff-id name required-power]} (l 111 (<=sub [:sv.weapons.stuff/view]))]
      (let [{:keys [is-on percentage is-ready]} (l 33333 (<=sub [:sv.weapon/readiness {:stuff-id stuff-id}]))]
        ^{:key stuff-id}
        [:div.weapon {:class (str (if is-on "with-power" "without-power")
                                  (when is-ready " is-ready"))
                      :on-click #(=>evt [:sv.weapon/power-up {:stuff-id stuff-id}])
                      :on-context-menu #(do (.preventDefault %)
                                            (=>evt [:sv.weapon/power-down {:stuff-id stuff-id}]))
                      }
         [:div.readiness
          [:div.bar {:style {:height (str percentage "%")}}]]
         [:div.box
          [:div.power-require
           (repeat required-power [:div.cell])]
          [:div.name name]]])))])

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
      (let [{:keys [left max]} (<=sub [:sv.power/info])]
        (map-indexed (fn [idx el]
                       ^{:key idx}
                       [:div.cell {:class (if (< idx left) "with-power" "without-power")}])
                     (repeat max {})))]

     (for [{:keys [system-id with-power max in-use]} (<=sub [:view.sv/systems])]
       ^{:key system-id}
       [:div.module
        [:div.icon {:class (str (name system-id) " " (if (not= 0 in-use)
                                                       "with-power"
                                                       "without-power"))
                    :on-click #(=>evt [:sv.system/power-up {:system-id system-id}])
                    :on-context-menu #(do (.preventDefault %)
                                          (=>evt [:sv.system/power-down {:system-id system-id}]))}]
        (map-indexed (fn [idx el]
                       ^{:key idx}
                       [:div.cell {:class (if (< idx in-use) "with-power" "without-power")}])
                     (repeat max {}))
        ])
     [weapons]]]
   [:div.player.enemy
    [:img.ship-mock {:src "./image/enemy-ship.png"}]]
   ])
