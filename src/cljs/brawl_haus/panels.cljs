(ns brawl-haus.panels
  (:require [re-frame.core :as rf]
            [brawl-haus.utils :refer [<sub l]]))

(defmulti panel :location-id)

(defmethod panel :default [location]
  (rf/dispatch [:tube/send [:race/attend]]) ;; Shift to server side
  [:div "Attending a race"])

(defn notifications []
  [:ul.notifications
   (for [{:keys [text type]} (<sub [:notification])]
     [:li.alert.alert-success {:key text :class type}
      text])])
