(ns brawl-haus.panels
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [brawl-haus.utils :refer [<sub l]]))

(defmulti panel (comp :location-id :location))

(defn thoughts []
  (let [active-dot (r/atom 5)]
    (js/setInterval #(swap! active-dot (fn [d] (rem (inc d) 7))) 200)
    (fn []
      [:h5.thoughts
       (doall
        (for [n [0 1 2 3 4]]
          ^{:key n}
          [:div.dot {:class (when (= n @active-dot)
                              "active")}]))])))

(defmethod panel :default [location]
  [:div.default-panel
   [thoughts]
   [:h2.face "(~_~)"]
   [:h5.title "Not there yet"]])
