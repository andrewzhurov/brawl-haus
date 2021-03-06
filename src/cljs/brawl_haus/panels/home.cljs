(ns brawl-haus.panels.home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub <=sub defview view]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]
            [brawl-haus.components :as comps]))

(defn shelf-item [{:keys [img-src name desc action-fn action]}]
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

(def shelf-items
  [#_{:name "Clojure Crash Course"
    :img-src "./image/clojure-men-s-premium-t-shirt.jpg"
    :desc "Dash to get you grasp the language. Heavily practice powered."
    :action "Go for it."
    :action-fn #(rf/dispatch [:conn/send [:ccc/attend]])}
   #_{:name "Sandbox (View)"
    :img-src "./image/little-sand-castle.jpg"
    :desc "Sand castles. This is the place you play around Hiccup and Garden - things powering your View"
    :action "Make a castle of my own"
    :action-fn #(rf/dispatch [:conn/send [:hiccup-touch/attend]])}
   {:name "At last"
    :img-src "./brawl/poster.jpg"
    :desc "And here you meet after all, any left to say?"
    :action "Words can hurt too."
    :action-fn #(rf/dispatch [:conn/send [:reunion/attend]])}
   {:name "Race game"
    :img-src "./image/graffiti-racer.jpg"
    :desc "Techdemo on what the architecture can facilitate. Multiplayer race-chunk-of-text game"
    :action "Ready? Set. GO!"
    :action-fn #(rf/dispatch [:conn/send [:race/attend]])}
   {:name "Space Versus"
    :img-src "https://i.kinja-img.com/gawker-media/image/upload/t_original/tm7dh3qlzvvnfefftvks.jpg"
    :desc "It's a versus... in the space! Machete kills."
    :action "Embark on a spaceship"
    :action-fn #(rf/dispatch [:conn/send [:sv/attend]])}
   {:name "Frozen in Time"
    :img-src "https://cdn.pixabay.com/photo/2018/01/18/00/11/winter-3089313_1280.jpg"
    :desc "Game mechanic concepts for the yet to be built game"
    :action "Check it out"
    :action-fn #(rf/dispatch [:conn/send [:frozen-in-time/attend]])}]
  )

(defmethod panels/panel :home-panel
  [_]
  [:div.home-panel
   [:div.content-section
    [:ul.shelf
     (map shelf-item shelf-items)]]])
