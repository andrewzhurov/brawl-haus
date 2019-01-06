(ns brawl-haus.fit.phys
  (:require [brawl-haus.fit.time :as time]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.utils :refer [l]]))

(defn per-s [val dt]
  (* (/ val 1000) dt))

(defn component [m v a]
  {:phys {:m m
          :v v
          :a a}})

(def push-x 1)
(defn push-v [ent [v-x v-y] & [max-x dt]]
  (update-in ent [:phys :v] (fn [[now-v-x now-v-y]]
                              (if (and max-x dt)
                                [((if (neg? max-x) max min) (+ now-v-x (per-s (* v-x push-x) dt)) (* max-x push-x))
                                 (+ now-v-y (per-s (* v-y push-x) dt))]
                                [(+ now-v-x (* v-x push-x)) (+ now-v-y (* v-y push-x))])
                              )))
(defn repel [[a-id a] [b-id b]]
  (let [v-x (get-in a [:phys :v 0]) ]
    {a-id (push-v a [(- (* 2 v-x)) 0] 10)
     b-id (push-v b [(/ v-x 1.2) 0] 10)}))

(defn throttle [ent dt]
  (update-in ent [:phys :v] (fn [[v-x v-y]]
                              [(if (< (Math.abs v-x) 0.3) 0
                                   (* v-x (- 1 (per-s 1 dt))))
                               v-y
                               #_(* v-y (- 1 (per-s 3 dt)))])))


(def domain-mapping 5)
(def system
  (fn [{:keys [time-passed entities]}]
    {:entities (->> entities
                    (filter (comp :phys val))
                    (map (fn [[id {[x y] :position
                                   {[vx vy] :v} :phys :as subj}]]
                           (let [rt (time/relative-time subj time-passed)]
                             {id {:position [(+ x (per-s (* vx domain-mapping) rt))
                                             (+ y (per-s (* vy domain-mapping) rt))]}}))
                         )
                    (apply merge))}))
