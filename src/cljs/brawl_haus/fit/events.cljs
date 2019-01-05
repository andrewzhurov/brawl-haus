(ns brawl-haus.fit.events
  (:require [brawl-haus.utils :refer [l]]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.player :as player]))

(def events
  {:mouse (fn [{:keys [mouse current-tick] :as db} [_ coords]]
            (merge db {:prev-mouse mouse
                       :angle-diff-at current-tick
                       :angle-diff (u/calc-angle mouse coords)
                       :mouse coords}))

   :add-ent (fn [db [_ ent]] (assoc-in db [:entities (:id ent)] ent))
   :set-controls (fn [db [_ id val]] (assoc-in db [:controls id] val))
   :controls (fn [db [_ action]] (update-in db [:entities :player] player/control action))
   })

(defn >evt [evt]
  (l ">evt" evt)
  (l -1 (swap! state/db (fn [{:keys [current-tick] :as db}]
                         (update-in db [:evt-history (inc current-tick)] u/conjv evt)))))

(defn process-evts [{:keys [evt-history] :as db} at-tick]
  (reduce (fn [acc-db [evt-id :as evt]]
            ((get events evt-id) acc-db evt))
          db
          (get evt-history at-tick)))
