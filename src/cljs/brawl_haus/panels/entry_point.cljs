(ns brawl-haus.panels.entry-point
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [brawl-haus.utils :refer [l <sub >evt =>evt defview view]]
   [brawl-haus.panels :as panels]
   [brawl-haus.panels.race]
   [brawl-haus.panels.hiccup-touch]
   [brawl-haus.panels.home]
   [brawl-haus.panels.ccc]
   [brawl-haus.components :as comps]
   [brawl-haus.shortcuts :as shortcuts]
   [brawl-haus.focus :as focus]))

(defview :entry-point
  (fn [{:keys [location]}]
    [:div.main-layout {:class (str (:location-id location))}
     [:nav.nav-section
      [:div.nav-wrapper
       [:a.brand-logo {:on-click #(=>evt [:home/attend])} "BrawlHaus"]
       [:ul#nav-mobile.right
        [:li.controls [:a {:on-click #(>evt [:help/show])}
                       [:i.material-icons "all_out"]]]]]]

     [:div.content-section
      [panels/panel location]]
     [comps/chat]
     [shortcuts/help]]))
