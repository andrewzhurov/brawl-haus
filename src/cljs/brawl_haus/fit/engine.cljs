(ns brawl-haus.fit.engine
  (:require [brawl-haus.fit.state :as state]
            [brawl-haus.fit.events :as events]
            [brawl-haus.fit.time :as time]
            [brawl-haus.utils :refer [l deep-merge]]
            [brawl-haus.fit.player :as player]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.gravity :as gravity]
            [brawl-haus.fit.collision :as collision]))

(l "BLAH" "BLEH")

(def systems
  [player/system
   phys/system
   gravity/system
   collision/system
   #_[[:chase]
      (fn [dt subjs db]
        (let [player (first (filter #(= :player (:type %)) subjs))
              enemy (first (filter #(= :enemy (:type %)) subjs))
              [disp-x _] (displacement (:position player) (:position enemy))]
          (if (neg? disp-x)
            [player (phys-v enemy [-1 0] -3 dt)]
            [player (phys-v enemy [ 1 0]  3 dt)])))]

                                        ;self-destruct
   (fn [db]
     (let [now (Date.now)
           subjs (filter (fn [[_ {{:keys [after spawn-time]} :self-destruct}]]
                           (and spawn-time
                                (< (+ spawn-time after) now)))
                         (:entities db))
           ]
       {:entities (reduce (fn [acc [id _]] (assoc acc id nil))
                          {}
                          subjs)}
       ))])

(defn run-systems [db]
  (reduce (fn [db sys-fn]
            (deep-merge db (sys-fn db)))
          db
          systems))


(defn tick! []
  (swap! state/db
         (fn [db]
           (let [;time-skewed (* 2 (reduce + (map (fn [[from till]] (- till from)) (:reverse-history @mnemo))))
                 prev-tick (:current-tick db)
                 current-tick (inc prev-tick)
                                        ;now (- (js/Date.now) time-skewed)
                                        ;prev-tick (or (:current-tick db) (- (js/Date.now) 1000))
                 dt 16 ;(- now prev-tick) ;; LIE
                        ]
             #_(if (:current-reverse @mnemo)
                 (-> db
                     (back-tick)
                     (drop-controls)))
             (-> (assoc db :current-tick current-tick)
                 (events/process-evts prev-tick)
                 (time/time-passed)
                 (run-systems)
                 #_(mnemo-tick db)))))
  )


(defonce sys-running (atom nil))
(defn stop-engine! []
  (when-let [sr @sys-running]
    (js/clearInterval sr)))

(defn start-engine! []
  (do (stop-engine!)
      (reset! sys-running (js/setInterval tick! 20))))
