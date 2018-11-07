(ns brawl-haus.core
  (:require
   [reagent.core :as reagent]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [re-frame.core :as rf]
   [config.core :as config]
   [brawl-haus.receiver]
   [brawl-haus.db :as db]
   [brawl-haus.ws]
   [brawl-haus.utils :refer [l <sub >evt <=sub =>evt defview view]]

   [brawl-haus.panels :as panels]
   [brawl-haus.panels.race]
   [brawl-haus.panels.hiccup-touch]
   [brawl-haus.panels.home]
   [brawl-haus.panels.ccc]
   [brawl-haus.panels.space-versus]
   [brawl-haus.panels.reunion]

   [brawl-haus.components :as comps]
   [brawl-haus.focus :as focus]
   [brawl-haus.shortcuts :as shortcuts]

   [brawl-haus.sandbox]))


(defn dev-setup []
  (when (:debug? config/env)
    (enable-console-print!)
    (println "dev mode")))

(declare init)
(defn entry-point []
  [:div.main-layout
   [:nav.nav-section
    [:div.nav-wrapper
     [:a.brand-logo {:on-click #(=>evt [:home/attend])} "BrawlHaus"]
     [:ul#nav-mobile.right
      [:li [:a {:on-click #(do (rf/dispatch-sync [:conn/send [:reset]])
                               (js/setTimeout init 1000)
                               )} "RESET"]]
      [:li.controls [:a {:on-click #(>evt [:help/show])}
                     [:i.material-icons "all_out"]]]]]]
   [:div.content-section
    [panels/panel (<=sub [:personal-info])]]
   [comps/chat]
   [shortcuts/help]])

(defn mount-root []
  (reagent/render #_[brawl-haus.sandbox/content]
                  [entry-point]
                  (.getElementById js/document "app")))

(rf/reg-event-db
 :initialize-db
 (fn-traced [_ _]
            db/default-db))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:conn/create])
  (dev-setup)
  (shortcuts/reg-press-handlers)
  (rf/clear-subscription-cache!)
  (<=sub [:self])
  (<=sub [:my-subs])
  (mount-root))
