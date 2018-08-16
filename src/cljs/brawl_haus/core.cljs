(ns brawl-haus.core
  (:require
   [reagent.core :as reagent]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [brawl-haus.views :as views]
   [brawl-haus.config :as config]
   [brawl-haus.focus :as focus]
   [brawl-haus.receiver]
   [brawl-haus.db :as db]
   [brawl-haus.ws]
   [brawl-haus.tube]
   [brawl-haus.utils]
   [brawl-haus.shortcuts :as shortcuts]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(rf/reg-event-db
 :initialize-db
 (fn-traced [_ _]
            db/default-db))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])

  (rf/dispatch-sync shortcuts/keydown-rules)
  (rf/dispatch-sync shortcuts/keyup-rules)
  #_(re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [:conn/create])
  #_(re-frame/dispatch-sync [:tube/create])
  (focus/reg-focus-listener)
  (dev-setup)
  (mount-root))
