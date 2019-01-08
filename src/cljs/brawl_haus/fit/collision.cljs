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

(defmulti collide (fn [db [_ act] [_ pass]] (do #_(js/console.log "COLLIDED:" act pass)
                                                [(:type act) (:type pass)])))


(defmethod collide :default [_ _ _] nil)


;; FUCKED ON LOAD
;; Up to 22 ms
(def system
  (fn [{:keys [entities] :as db} & without]
    (let [without (or without #{})
          actor (some (fn [[ent-id ent]]
                        (when (and (get-in ent [:collision :actor?])
                                   (not (without ent-id)))
                          [ent-id ent]))
                      (:entities db))
          coll (when actor
                 (some (fn [[ent-id ent]]
                         (and (not ((conj without actor) ent-id))
                              (collided? actor [ent-id ent])
                              (collide db actor [ent-id ent])))
                       (:entities db)))
          ]
      (cond (not-empty coll)
            (recur
             (deep-merge db coll)
             (conj without (first actor)))

            (and actor (empty? coll))
            (recur db (conj without (first actor)))

            :else
             db))))
