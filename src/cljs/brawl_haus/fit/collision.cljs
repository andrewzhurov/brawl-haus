(ns brawl-haus.fit.collision
  (:require [brawl-haus.utils :refer [deep-merge l]]
            [brawl-haus.fit.player :as player]))

(defn component [actor?]
  {:collision {:actor? actor?
               :grounded? false}})

(defn collided? [{[act-x act-y] :position
                  [act-w act-h] :size}
                 {[pass-x pass-y] :position
                  [pass-w pass-h] :size}]
  (let [act-x2 (+ act-x act-w)
        act-y2 (+ act-y act-h)
        pass-x2 (+ pass-x pass-w)
        pass-y2 (+ pass-y pass-h)
        ]
    (not (or (or (> pass-x act-x2)
                 (< pass-x2 act-x))
             (or (> pass-y act-y2)
                 (< pass-y2 act-y))))))

(defmulti collide (fn [act pass] [(:type act) (:type pass)]))
(defmethod collide [:player :floor]
  [player floor]
  [(player/player-ground player floor)
   floor])

(defmethod collide [:player :stair]
  [{[w h] :size
    [player-x _] :position :as player}
   {[_ stair-y] :position :as stair}]
  [(player/player-ground player stair) stair])
(defmethod collide [:player :enemy]
  [p e]
  (println "HIT") [p e])

(defmethod collide :default [a b] [a b])



(def system
  (fn [db & without-subjs]
    (let [without-subjs (or without-subjs #{})
          subjs (->> (:entities db)
                     (remove (fn [[id _]] (contains? without-subjs id)))
                     (filter (comp :collision val)))
          actor (first (filter (comp :actor? :collision val) subjs))
          coll-subj (some #(when (and actor (collided? (second actor) (second %))) %)
                          (disj (set subjs) actor))
          actor (second actor)
          coll-subj (second coll-subj)]
      (if coll-subj
        (recur
         (deep-merge db
                     {:entities (if coll-subj
                                  (reduce (fn [acc {:keys [id] :as ent}] (assoc acc id ent))
                                          {}
                                          (collide actor coll-subj))
                                  {})})
         (conj without-subjs (:id coll-subj) (:id actor)))
        db))))
