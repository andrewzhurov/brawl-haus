(ns brawl-haus.panels.ccc
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub defview view]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]
            [brawl-haus.components :as comps]
            [paren-soup.core :as ps]
            [hiccups.runtime :as hiccups]
            [garden.core :as garden]
            #_[brawl-haus.events :as events]))

(defmethod panels/panel :ccc-panel
  [_]
  [:div.ccc-panel
   [:h4 "I'm glad you're interested"]
   [:div "Clojure Crash Course is currently under heavy development. "]
   [:div "It is in priority and will be the next thing to launch."]
   [:div "Keep in touch.;)"]])

