(ns brawl-haus.fit.collision
  (:require [brawl-haus.utils :refer [deep-merge l]]
            ))

(defn component [actor?]
  {:collision {:actor? actor?
               :grounded? false}})

(defn collided? [[_ {[act-x act-y] :position
                     [act-w act-h] :size}]
                 [_ {[pass-x pass-y] :position
                     [pass-w pass-h] :size}]]
  (let [act-x2 (+ act-x act-w)
        act-y2 (+ act-y act-h)
        pass-x2 (+ pass-x pass-w)
        pass-y2 (+ pass-y pass-h)
        ]
    (not (or (or (> pass-x act-x2)
                 (< pass-x2 act-x))
             (or (> pass-y act-y2)
                 (< pass-y2 act-y))))))

(defmulti collide (fn [[_ act] [_ pass]] (do #_(js/console.log "COLLIDED:" act pass)
                                             [(:type act) (:type pass)])))


(defmethod collide :default [_ _] nil)



(defn calc-collides [ents & worked-ents]
  (let [worked-ents (or worked-ents #{})
        subjs (into {} (remove (comp worked-ents key) ents))
        actor (first (filter (comp :actor? :collision val) subjs))
        coll (some #(and actor
                         (collided? actor %)
                         (collide actor %))
                   (disj (set subjs) actor))
        ]
    (cond coll
          (recur
           (deep-merge ents coll)
           (conj worked-ents (key actor)))

          (and actor (nil? coll))
          (recur ents (conj worked-ents (key actor)))

          :else
          ents)))

(def system
  (fn [{:keys [entities]}]
    (let [subjs (into {} (filter (comp :collision val) entities))]

      {:entities (calc-collides subjs)})))
