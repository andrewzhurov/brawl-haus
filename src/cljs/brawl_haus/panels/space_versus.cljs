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


(fn [number-of-li]
  [:div
   [:ul
    [:li]
    [:li]]])

(defn weapons []
  [:div.weapons-panel
   (doall
    (for [{:keys [stuff-id name required-power]} (<=sub [:sv.weapons.stuff/view])]
      (let [{:keys [is-on is-selected percentage is-ready]} (<=sub [:sv.weapon/readiness stuff-id])]
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


(defn ship-obscured [{:keys [ship-id systems weapons]}]
  [:div.ship
   [:img.ship-backdrop {:src "https://vignette.wikia.nocookie.net/ftl/images/a/aa/Kestrel_ship.png/revision/latest?cb=20160308183246"}]
   [:div.ship-schema
    (for [{:keys [id status]} systems]
      [:div.system {:class (str (name id) " " status)
                    :on-click #(=>evt [:sv.weapon/hit ship-id id])}])
    (for [{:keys [slot is-on is-ready]} weapons]
      [:img.hardware-weapon {:class (str (when is-on " is-on")
                                         (when is-ready " is-ready")
                                         #_(when is-ready " firing")
                                         " w" slot
                                         )

                             :src "https://vignette.wikia.nocookie.net/ftl/images/9/99/Basic_Laser.png/revision/latest?cb=20141122223317"}])]])

(defmethod panels/panel :space-versus
  [{:keys [conn-id]
    {{:keys [ship-location-id]} :params} :location}]
  [:div.space-versus
   [:div.me
    [ship-obscured (<=sub [:view.sv/ship conn-id])]
    [:div.bottom-hud
     [:div.energy-bar
      (let [{:keys [in-use left]} (<=sub [:sv.power/info])]
        (map-indexed (fn [idx status]
                       ^{:key idx}
                       [:div.cell {:class status}])
                     (concat (repeat left "with-power")
                             (repeat in-use "without-power"))))]

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
        ])
     [weapons]]]

   [:div.outside
    [:div.locations.collection
     (for [{:keys [location-id location-name ship-captains]} (<=sub [:view.sv/locations])]
       ^{:key location-id}
       [:a.collection-item {:on-click #(=>evt [:sv/attend {:location location-id}])}
        location-name (pr-str ship-captains)])]

    [:div.ships
     (for [ship-info (<=sub [:view.sv/ships ship-location-id])
           :when (not= (:ship-id ship-info) conn-id)]
       [ship-obscured ship-info])]]])
