(ns brawl-haus.sandbox2
  (:require [garden.core]
            [reagent.core :as r]
            [reagent.ratom :as ra]
            [re-frame.core :refer [reg-event-db reg-sub reg-sub-raw]]
            [brawl-haus.utils :refer [l deep-merge]]
            [brawl-haus.panels :as panels]
            [goog.string :as gstr]
            [brawl-haus.fit.events :refer [>evt]]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.gravity :as gravity]
            [brawl-haus.fit.chase :as chase]
            [brawl-haus.fit.css :as css]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.player :as player]
            [brawl-haus.fit.time :as time]
            [brawl-haus.fit.mnemo :as mnemo]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.engine :as engine]
            [brawl-haus.fit.lifecycle :as lifecycle]
            [brawl-haus.fit.render :as render]))


(defn content []
  (r/create-class
   {:component-will-mount lifecycle/init
    :component-will-unmount lifecycle/destroy
    :reagent-render
    (fn [_]
      [:div
       [:style (garden.core/css css/styles)]
       [:svg {:style {:width "100vw"
                      :height "100vh"}
              :on-mouse-move #(when (mnemo/normal-time?) (>evt [:mouse (u/coords %)]))
              :on-mouse-down #(>evt [:set-controls :trigger true])
              :on-mouse-up #(>evt [:set-controls :trigger false])
              }
        [:defs
         [:linearGradient#grad1
          {:y2 "0%", :x2 "100%", :y1 "0%", :x1 "0%"}
          [:stop
           {:style {:stop-color "rgb(255,100,0)"
                    :stop-opacity "0"},
            :offset "0%"}]
          [:stop
           {:style {:stop-color "rgb(255,100,0)"
                    :stop-opacity "0.4"},
            :offset "100%"}]]]
        #_[timeline]
        #_[tutorial]
        (for [[id ent] (sort-by key (:entities @state/db))]
          ^{:key id}
          [render/render ent])
        [u/inspect (get-in @state/db [:entities :player :phys]) 10 50]
        #_[:text {:x 100 :y 100} @state/angle]
        ]])}))


(defmethod panels/panel :frozen-in-time
  [_]
  [content]
  )
