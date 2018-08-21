(ns brawl-haus.core
  (:require
   [reagent.core :as reagent]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [brawl-haus.config :as config]
   [brawl-haus.focus :as focus]
   [brawl-haus.receiver]
   [brawl-haus.db :as db]
   [brawl-haus.ws]
   [brawl-haus.utils :refer [view]]
   [brawl-haus.panels.entry-point]
   [brawl-haus.shortcuts :as shortcuts]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [view :entry-point]
                  (.getElementById js/document "app")))

(rf/reg-event-db
 :initialize-db
 (fn-traced [_ _]
            db/default-db))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:conn/create])
  (focus/reg-focus-listener)
  (dev-setup)
  (shortcuts/reg-press-handlers)
  (mount-root))
