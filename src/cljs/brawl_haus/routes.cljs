(ns brawl-haus.routes
  (:import goog.History)
  (:require
   [goog.events :as gevents]
   [goog.history.EventType :as EventType]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [brawl-haus.events :as events]
   [bidi.bidi :as bidi]))
(defn l [desc expr] (js/console.log desc expr) expr)
#_(def route-map
  ["/" {"" :race-panel #_:home-panel
        "race" :races-panel
        ["race/" :race-id] :race-panel
        "login" :login-panel}])

#_(rf/reg-event-db
 :current-route
 (fn [db [_ & route-parts]]
   (set! (.-location js/window)
         (str "#/"
              (clojure.string/join "/" (map (fn [part]
                                              (if (keyword? part)
                                                (name part)
                                                part))
                                            route-parts))))
   db))

#_(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (js/console.log "Route token now:" event (.-token event))
       (rf/dispatch [:db/set-in [:current-panel] (l "Match:" (bidi/match-route route-map (.-token event)))])))
    (.setEnabled true)))

(defn app-routes []
  #_(hook-browser-navigation!))
