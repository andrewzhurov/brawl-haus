(ns brawl-haus.panels.races
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [brawl-haus.panels.race :as race]))

(rf/reg-sub
 :highscored-users
 :<- [:users]
 (fn [users _]
   (->> users
        (l 1)
        (filter :highscore)
        (l 2)
        (sort-by :highscore)
        reverse
        (l 3))))

(defn highscores []
  [:div.highscores.collection.z-depth-1
   (for [{:keys [nick highscore]} (<sub [:highscored-users])]
     [:a.collection-item {:key nick} nick
      [:span.badge.white-text highscore]])])

(defn open-races []
  [:div.collection.open-races
   (for [[_ {:keys [id initiator participants status] :as race}] (<sub [:db/get-in [:public-state :open-races]])
         :when (not= :ongoing status)]
     [:a.collection-item.race {:key id
                               :on-click #(do (rf/dispatch [:enter-race id])
                                              (rf/dispatch [:current-route :race id]))}
      [race/countdown race]
      (clojure.string/join ", " (keys participants))])])



(defmethod panels/panel :races-panel
  [_ route-params]
  [:div.app
   [panels/navbar]
   [:div.content.races-panel
    [:div.new-race-btn.btn {:on-click #(rf/dispatch [:tube/send [:new-race]])}
     "New race!"]
    [open-races]
    [highscores]]])
