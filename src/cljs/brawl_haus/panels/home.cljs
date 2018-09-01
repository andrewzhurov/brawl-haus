(ns brawl-haus.panels.home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub defview view]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]
            [brawl-haus.components :as comps]))

(defn shelf-item [{:keys [name desc action-fn action]}]
  [:li.card.blue-grey.darken-1
   [:div.card-content.white-text
    [:span.card-title name]
    [:p desc]]
   [:div.card-action
    [:a {:on-click action-fn} action]]])

(defn shelf-item2 [{:keys [img-src name desc action-fn action]}]
  ^{:key name}
  [:li.col.s12.m7
   [:div.card
    [:div.card-image
     [:img {:src img-src}]
     [:span.card-title name]]
    [:div.card-content
     [:p
      desc]]
    [:div.card-action {:on-click action-fn}
     [:a action]]]])

[:li "Clojure crash course"]
[:li "View sandbox"]
[:li "Race game"]

(def shelf-items
  [{:name "Clojure Crash Course"
    :img-src "./image/clojure-men-s-premium-t-shirt.jpg"
    :desc "Dash to get you grasp the language. Heavily practice powered."
    :action "Go for it."
    :action-fn #(rf/dispatch [:conn/send [:ccc/attend]])}
   {:name "Sandbox (View)"
    :img-src "./image/little-sand-castle.jpg"
    :desc "Sand castles. This is the place you play around Hiccup and Garden - things powering your View"
    :action "Make a castle of my own"
    :action-fn #(rf/dispatch [:conn/send [:hiccup-touch/attend]])}
   {:name "Race game"
    :img-src "./image/graffiti-racer.jpg"
    :desc "Techdemo on what the architecture can facilitate. Multiplayer race-chunk-of-text game"
    :action "Ready? Set. GO!"
    :action-fn #(rf/dispatch [:conn/send [:race/attend]])}
   {:name "Space Versus"
    :img-src "https://i.kinja-img.com/gawker-media/image/upload/t_original/tm7dh3qlzvvnfefftvks.jpg"
    :desc "It's a versus... in the space! Machete kills."
    :action "Embark on a spaceship"
    :action-fn #(rf/dispatch [:conn/send [:space-versus/attend]])}])

(defmethod panels/panel :home-panel
  [_]
  [:div.home-panel
   [:div.content-section
    [:ul.shelf
     (map shelf-item2 shelf-items)
     ]]])

