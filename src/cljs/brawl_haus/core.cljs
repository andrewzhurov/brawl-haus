(ns brawl-haus.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [brawl-haus.events :as events]
   [brawl-haus.routes :as routes]
   [brawl-haus.views :as views]
   [brawl-haus.config :as config]
   [brawl-haus.tube]
   [brawl-haus.ws]
   [brawl-haus.utils]
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
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [:tube/connect])
  (re-frame/dispatch-sync [:tube/send [:init]])
  (dev-setup)
  (mount-root))
