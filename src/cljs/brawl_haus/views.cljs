(ns brawl-haus.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [re-pressed.core :as rp]
   [brawl-haus.subs :as subs]
   [brawl-haus.utils :refer [l <sub]]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels.race]))

(defn main-panel []
  (let [tube (<sub [:tube])
        location (<sub [:db/get-in [:public-state :user-locations (:tube/id tube)]])]
    [:div {:key (str location)}
     (pr-str location)
     #_[panels/notifications]
     [panels/panel location]]))
