(ns brawl-haus.fit.time
  (:require [brawl-haus.fit.state :refer [db]]))

(def fps 30)
(def dt (/ 1000 fps))


(def default-time-flow 0)
(defn relative-time [ent dt]
  (cond
    (= :player (:id ent)) (* dt 2)
    (= :bullet (:type ent)) dt
    :else (/ dt 4)))

(defn time-passed [{:keys [current-tick angle-diff-at angle-diff]
                    {{{[vx vy] :v} :phys} :player} :entities :as db}]
  (let [disp-x (Math.abs vx)
        disp-y (Math.abs vy)
        curr-angle-diff (if (= angle-diff-at current-tick) (Math.abs angle-diff) 0)]
    (merge db
           {:time-passed (+ default-time-flow
                            (* 3 disp-x)
                            disp-y
                            (/ curr-angle-diff 4))})))


