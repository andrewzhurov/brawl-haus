(ns brawl-haus.panels.stand-by
  (:require [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]))

;; Duplicate
(defn in-race? [race user]
  (contains? (:participants race) (:nick user)))

(defn in-races [races user]
  (filter #(in-race? % user) races))

(defmethod panels/panel :stand-by-panel [location]
  (let [[_ race] (<sub [:race-to-be])
        user (<sub [:user])]
    (cond
      (nil? race)
      (do (rf/dispatch [:tube/send [:new-race]])
          [:div "Waiting for a race to init..."])

      ;; Duplicate
      (not (in-race? race user))
      (do (rf/dispatch [:tube/send [:enter-race (:nick user)]])
          [:div "Entering a race..."])

      :waiting-sync
      [:div "Please stand by"])))
