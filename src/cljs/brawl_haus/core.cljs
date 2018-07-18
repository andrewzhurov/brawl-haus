(ns brawl-haus.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [brawl-haus.events :as events]
   [brawl-haus.views :as views]
   [brawl-haus.config :as config]
   [brawl-haus.receiver]
   [brawl-haus.tube]
   [brawl-haus.utils]
   [brawl-haus.shortcuts :as shortcuts]
   ))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])

  (re-frame/dispatch-sync shortcuts/keydown-rules)
  (re-frame/dispatch-sync shortcuts/keyup-rules)
  #_(re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [:tube/create])
  (dev-setup)
  (mount-root))
