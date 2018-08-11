(ns brawl-haus.panels
  (:require [re-frame.core :as rf]
            [brawl-haus.utils :refer [<sub l]]))

(defmulti panel :location-id)

(defmethod panel :default [location]
  [:div "Not there yet ~_~"])

(defn notifications []
  [:ul.notifications
   (for [{:keys [text type]} (<sub [:notification])]
     [:li.alert.alert-success {:key text :class type}
      text])])
