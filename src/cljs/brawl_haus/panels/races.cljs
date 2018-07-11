(ns brawl-haus.panels.races
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]))

(defn open-races []
  [:div.collection.open-races
   (for [[_ {:keys [id initiator participants] :as race}] (<sub [:db/get-in [:public-state :open-races]])]
     [:a.collection-item.race {:key id
                               :on-click #(do (rf/dispatch [:enter-race id])
                                              (rf/dispatch [:current-route :race id]))}
      (js/console.log "RACE:" race)
      (str "(" (count participants) ") " id)])])



(defmethod panels/panel :races-panel
  [_ route-params]
  (l "Route params:" route-params)
  (let [race (<sub [:race (l "Race id:" (:race-id route-params))])]
    [:div.app
     [panels/navbar]
     [:div.content.races-panel
      [:div.new-race-btn.btn {:on-click #(rf/dispatch [:tube/send [:new-race]])}
       "New race!"]
      [open-races]]]))
