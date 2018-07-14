(ns brawl-haus.panels
  (:require [brawl-haus.utils :refer [<sub l]]))

(defmulti panel :location-id)

(defmethod panel :default [location]
  [:div "None panels registered for: " (pr-str location)])

(defn home-btn []
  [:a.home-btn.btn {:href "/#/"
                    :title "Home"}
   [:i.material-icons
    "home"]])

(defn notifications []
  [:ul.notifications
   (for [{:keys [text type]} (<sub [:notification])]
     [:li.alert.alert-success {:key text :class type}
      text])])

(defn navbar []
  [:div.navbar.z-depth-3
   [:a.tab.btn {:title "Home"
                :href "#/"}
    [:i.material-icons  "home"]]
   [:a.tab.btn {:title "Race"
                :href "#/race"}
    [:i.material-icons  "drive_eta"]]])
