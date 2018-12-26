(ns brawl-haus.systems.collision)

(defn collided?
  [{position1 :position
    collision1 :collision :as ent1}
   {position2 :position
    collision2 :collision :as ent2}]
  (and (not= ent1 ent2)
       (> (+ (:r collision1) (:r collision2))
          (hypotenuse (shift position1 position2)))))
(defmulti collide (fn [act ent] [(:type act) (:type ent)]))

(defmethod collide :default
  [ent1 ent2]
  [ent1 ent2])

(defn sys [ents]
  (let [{actors true
         passive false
         others nil} (group-by #(get-in % [:collision :actor?]) ents)
        subjects (into actors passive)
        [act subj] (loop [[act & acts :as all-acts] actors
                          [subj & subjs :as s] subjects]
                     (cond (nil? act)
                           nil
                           
                           (nil? subj)
                           (recur acts subjects)
                           
                           (= act subj)
                           (recur all-acts subjs)
                           
                           (collided? act subj)
                           [act subj]

                           :not
                           (recur all-acts subjs)))]
    (if (and act subj)
      (into (disj ents act subj) (collide act subj))
      ents)
    #_(loop [acc others
             [act & rest-acts] actors
             [pas & rest-pass] passive]
        (if false
          ent)))
  
  
  #_(let [{actors true
           passive false
             others nil} (group-by #(get-in % [:collision :actor?]) ents)
          ]
      (->> (time (reduce (fn [ents act]
                           (mapcat (fn [ent]
                                     (if false #_(collided? act ent)
                                         (collide act ent)
                                         [act ent]))
                                   ents))
                         passive actors))
           (into others)
           (into #{})))
  )
