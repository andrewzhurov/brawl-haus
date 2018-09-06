(ns brawl-haus.panels.space-versus
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub >evt <=sub =>evt defview view]]
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
      (let [{:keys [is-on is-selected percentage is-ready]} (l 33333 (<=sub [:sv.weapon/readiness stuff-id]))]
        ^{:key stuff-id}
        [:div.weapon {:class (str (if is-on "with-power" "without-power")
                                  (when is-selected " is-selected")
                                  (when is-ready " is-ready"))
                      :on-click (if-not is-on
                                  #(=>evt [:sv.weapon/power-up stuff-id])
                                  #(=>evt [:sv.weapon/select stuff-id]))
                      :on-context-menu (if is-selected
                                         #(do (.preventDefault %)
                                              (=>evt [:sv.weapon/unselect stuff-id]))
                                         #(do (.preventDefault %)
                                              (=>evt [:sv.weapon/power-down stuff-id])))
                      }
         [:div.readiness
          [:div.bar {:style {:height (str percentage "%")}}]]
         [:div.box
          [:div.power-require
           (repeat required-power [:div.cell])]
          [:div.name name]]])))])

(defn ship-ui [ship-id]
  ^{:key ship-id}
  [:div.ship
   [:div.systems
    (for [{:keys [id]} (<=sub [:view.sv/systems ship-id])]
      [:div.system {:class (name id)
                    :on-click #(=>evt [:sv.weapon/hit ship-id id])}])]
   ])

(defmethod panels/panel :space-versus
  [{{:keys [space-versus-id]} :params :as all}]
  (l 111 all)
  [:div.space-versus
   [:div.player.me
    [:div.ship
     [:img.ship-backdrop {:src "https://vignette.wikia.nocookie.net/ftl/images/a/aa/Kestrel_ship.png/revision/latest?cb=20160308183246"}]
     [:div.ship-schema
      [:div.system.engines]
      [:div.system.shields]
      [:div.system.weapons]
      (for [{:keys [slot is-on is-ready]} (l 666666 (<=sub [:sv.weapons/readiness]))]
        [:img.hardware-weapon {:class (str (when is-on " is-on")
                                           (when is-ready " is-ready")
                                           " w" slot)

                               :src "https://vignette.wikia.nocookie.net/ftl/images/9/99/Basic_Laser.png/revision/latest?cb=20141122223317"}])]]
    #_[:img.ship-mock {:src "./image/my-ship.jpg"}]
    (doall
     (for [id (l "ENEMIES:" (<=sub [:sv/enemies]))]
       [ship-ui id]))
    [:div.bottom-hud
     [:div.energy-bar
      (let [{:keys [in-use left]} (<=sub [:sv.power/info])]
        (map-indexed (fn [idx status]
                       ^{:key idx}
                       [:div.cell {:class status}])
                     (concat (repeat left "with-power")
                             (repeat in-use "without-power"))))]

     (for [{:keys [id status idle in-use damaged]} (l 111111 (<=sub [:view.sv/systems]))]
       ^{:key id}
       [:div.module
        (map-indexed (fn [idx status]
                       ^{:key idx}
                       [:div.cell {:class status}])
                     (concat (repeat damaged "damaged")
                             (repeat idle "without-power")
                             (repeat in-use "with-power")))
        [:div.icon {:class (str (name id) " " (if (not= 0 in-use)
                                                "with-power"
                                                "without-power"))
                    :on-click #(=>evt [:sv.system/power-up id])
                    :on-context-menu #(do (.preventDefault %)
                                          (=>evt [:sv.system/power-down id]))}]
        ])
     [weapons]]]
   [:div.player.enemy
    [:img.ship-mock {:src "./image/enemy-ship.png"}]]
   ])
