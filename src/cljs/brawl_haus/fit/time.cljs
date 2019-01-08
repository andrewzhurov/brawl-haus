(ns brawl-haus.fit.time
  (:require [brawl-haus.fit.state :refer [db]]))

(def fps 30)
(def dt (/ 1000 fps))


(def default-time-flow 25)
(defn relative-time [ent dt]
  (cond
    (= :player (:id ent)) (* 2 dt)
    (= :bullet (:type ent)) (* 3 dt)
    :else dt #_(/ dt 4)))

(defn time-passed [{:keys [current-tick angle-diff-at angle-diff]
                    {{{[vx vy] :v} :phys} :player} :entities :as db}]
  (let [disp-x (Math.abs vx)
        disp-y (Math.abs vy)
        curr-angle-diff (if (= angle-diff-at current-tick) (Math.abs angle-diff) 0)]
    (-> db
        (assoc :time-passed 30 #_(+ #_default-time-flow
                                (if (not= 0 disp-x) 10 0)
                                (if (not= 0 disp-y) 10 0)
                                (/ curr-angle-diff 4)))
        (update :time-passed-all + 30))))


