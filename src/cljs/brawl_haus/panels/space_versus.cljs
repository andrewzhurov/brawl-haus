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
    (for [{:keys [stuff-id required-power] weapon-name :name} (<=sub [:sv.weapons.stuff/view])]
      (let [{:keys [status percentage]} (<=sub [:sv.weapon/readiness stuff-id])]
        ^{:key stuff-id}
        [:div.weapon {:class (and status (name status))
                      :on-click (if (= :idle status)
                                  #(=>evt [:sv.weapon/power-up stuff-id])
                                  #(=>evt [:sv.weapon/select stuff-id]))
                      :on-context-menu (if (:selected status)
                                         #(do (.preventDefault %)
                                              (=>evt [:sv.weapon/unselect stuff-id]))
                                         #(do (.preventDefault %)
                                              (=>evt [:sv.weapon/power-down stuff-id])))}
         [:div.readiness
          [:div.bar {:style {:height (str percentage "%")}}]]
         [:div.box
          [:div.power-require
           (repeat required-power [:div.cell])]
          [:div.name weapon-name]]])))])


(defn locations []
  [:div.locations.collection
   [:div.btn.jump {:on-click #(=>evt [:sv/jump (str (random-uuid))])} "TO RANDOM"]
   (doall
    (for [location-id (<=sub [:sv/locations])]
      (let [{:keys [location-id location-name station ships]} (<=sub [:sv.location/details location-id])]
        ^{:key location-id}
        [:a.collection-item {:on-click #(=>evt [:sv/jump location-id])}
         (str location-name) "  S:" (count ships)])))])

(defn bottom-hud []
  [:div.bottom-hud
    [:div.energy-bar
     (let [{:keys [in-use left]} (<=sub [:sv.power/info])]
       (map-indexed (fn [idx status]
                      ^{:key idx}
                      [:div.cell {:class status}])
                    (concat (repeat left "with-power")
                            (repeat in-use "without-power"))))]

   (doall
    (for [{:keys [id status idle in-use damaged]} (<=sub [:view.sv/systems])]
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
       ]))
    [weapons]])

(defn obscured-weapon [ship-id weapon-id]
  ^{:key weapon-id}
  (let [{:keys [slot status]} (<=sub [:sv.weapon/obscured-readiness ship-id weapon-id])]
    (do
      (println "Weapon status:" slot status)
      (when (and (= ship-id (:conn-id (<=sub [:self])))
                 (= :ready status))
        (.play (js/Audio. "ftl-assets/audio/waves/ui/select_light1.wav")))
      (when (= :firing status)
        (.play (js/Audio. "ftl-assets/audio/waves/weapons/bp_laser_3c.ogg")))
      ^{:key slot}
      [:div.hardware-weapon {:class (str (and status (name status)) " w" slot)}
       [:img {:src "https://vignette.wikia.nocookie.net/ftl/images/9/99/Basic_Laser.png/revision/latest?cb=20141122223317"}]
       [:div.ready-indicator]
       [:div.charge]])))

(defn obscured-system [ship-id system-id]
  (let [{:keys [status last-hit-at_FOR_UPDATE_SEND] :as all} (<=sub [:sv.system/status ship-id system-id])]
    (println "System status:" ship-id system-id status)
    ^{:key last-hit-at_FOR_UPDATE_SEND}
    [:div.system {:id last-hit-at_FOR_UPDATE_SEND
                  :class (str (name system-id)
                              " " status)
                  :on-click #(=>evt [:sv.weapon/hit ship-id system-id])}]))

(defn ship-obscured [ship-id]
  (l "BEING RAN:" ship-id)
  ^{:key ship-id}
  [:div.ship {:class (when (<=sub [:sv.ship/wrecked? ship-id]) "wrecked")}
   [:div.name (<=sub [:sv.ship/name ship-id])]
   (let [{:keys [status percentage]} (<=sub [:sv.shield/readiness ship-id])]
     [:div.shield {:class status
                   :style {:opacity (/ percentage  100)}}])
   [:img.ship-backdrop {:src "https://vignette.wikia.nocookie.net/ftl/images/a/aa/Kestrel_ship.png/revision/latest?cb=20160308183246"}]
   [:div.ship-schema
    (doall
     (for [system-id (<=sub [:sv.ship/systems ship-id])]
       [obscured-system ship-id system-id]))
    (doall
     (for [weapon-id (<=sub [:sv.ship/weapons ship-id])]
       [obscured-weapon ship-id weapon-id]))]])

(defn current-location []
  (let [{:keys [ships station]} (<=sub [:sv.current-location/details])]
    [:div.current-location
     (when station
       [:div.station "STATION"
        [:div
         "Purchase weapon:"]
        [:div.store
         [:div.weapon {:on-click #(=>evt [:sv.store/purchase])}
          [:div.name "Basic Laser"]
          [:div.price 30]]]])
     (doall
      (for [ship-id ships]
        (do (l "INSIDE FOR:" ship-id)
            [ship-obscured ship-id]
            )))
     ]))

(defmethod panels/panel :space-versus
  [{:keys [conn-id]}]
  [:div.space-versus
   [:div.ship
    [ship-obscured conn-id]]
   [locations]
   [current-location]

   [bottom-hud]
   ])
