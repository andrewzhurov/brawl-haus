(ns brawl-haus.focus
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.utils :refer [l <sub]]))

(defonce focus-state (atom {:current-node nil
                            :previous-node nil}))

(defn reg-focus-listener []
  (js/document.addEventListener "focus"
                                (fn [e]
                                  (swap! focus-state
                                         (fn [current-state]
                                           {:current-node (.-target e)
                                            :previous-node (:current-node current-state)})))
                                true))

(defn focus-previous []
  (some-> (:previous-node @focus-state)
          (.focus)))
