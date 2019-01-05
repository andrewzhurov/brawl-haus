(ns brawl-haus.fit.phys
  (:require [brawl-haus.fit.time :as time]))

(defn per-s [val dt]
  (* (/ val 1000) dt))

(defn component [m v a]
  {:phys {:m m
          :v v
          :a a}})

(defn push-v [ent [v-x v-y] & [max-x dt]]
  (update-in ent [:phys :v] (fn [[now-v-x now-v-y]]
                              (if (and max-x dt)
                                [((if (neg? max-x) max min) (+ now-v-x v-x) max-x)
                                 (+ now-v-y v-y)]
                                [(+ now-v-x v-x) (+ now-v-y v-y)])
                              )))

(defn throttle [ent dt]
  (update-in ent [:phys :v] (fn [[v-x v-y]]
                              [(if (< (Math.abs v-x) 0.3) 0
                                   (* v-x (- 1 (per-s 1 dt))))
                               v-y
                               #_(* v-y (- 1 (per-s 3 dt)))])))


(def system
  (fn [{:keys [time-passed entities]}]
    {:entities (->> entities
                    (filter (comp :phys val))
                    (map (fn [[id {[x y] :position
                                   {[vx vy] :v} :phys :as subj}]]
                           (let [rt (time/relative-time subj time-passed)]
                             {id {:position [(+ x (* (/ (* vx 3) 1000) rt)) (+ y (* (/ (* vy 3) 1000) rt))]}}))
                         )
                    (apply merge))}))
