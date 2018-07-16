(ns brawl-haus.panels.races
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]))

(rf/reg-sub
 :highscored-users
 :<- [:users]
 (fn [users _]
   (->> users
        (filter :highscore)
        (sort-by :highscore)
        reverse)))

(defn highscores []
  [:ul.highscores.collection.with-header.z-depth-1
   [:li.collection-header.z-depth-1
    [:h5 "Highscores"]]
   (for [{:keys [nick highscore]} (<sub [:highscored-users])]
     [:li.collection-item {:key nick} nick
      [:span.badge.white-text highscore]])])
