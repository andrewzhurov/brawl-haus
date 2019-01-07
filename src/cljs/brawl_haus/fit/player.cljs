(ns brawl-haus.fit.player
  (:require [brawl-haus.utils :refer [l deep-merge]]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.time :as time]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.weapon :as weapon]
            [brawl-haus.fit.chase :as chase]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]
            [brawl-haus.fit.mnemo :as mnemo]
            ))

(defn comp-person [spec]
  {:person (merge spec
                  {:desired-pose :stand})
   :size [(get-in spec [:poses :stand :width]) (get-in spec [:poses :stand :height])]})

(defn person-pose [ent & [pose]]
  (get-in ent [:person :poses (or pose (get-in ent [:person :desired-pose]))]))
(defn person-to-pose [person pose]
  (let [{:keys [width height]} (person-pose person pose)]
    (-> person
        (assoc-in [:person :desired-pose] pose)
        (assoc-in [:collision :grounded?] false)
        (merge {:size [width height]}))))

(defn move [ent move-direction dt]
  (let [{:keys [angle facing]} @state/angle ;; SIDECOFX
        backward? (not= move-direction facing)
        pose (person-pose ent)
        [speed max] (case [move-direction backward?]
                      [:left false] [[(- (get pose :front-speed)) 0] (- (get pose :front-max))]
                      [:left true]  [[(- (get pose :back-speed)) 0] (- (get pose :back-max))]
                      [:right false] [[(get pose :front-speed) 0] (get pose :front-max)]
                      [:right true] [[(get pose :back-speed) 0] (get pose :back-max)])]
    (phys/push-v ent speed max dt)))

(defn player-ground [{[player-x _] :position
                      [_ player-h] :size :as player}
                     {[_ floor-y] :position :as floor}]
  (deep-merge
   player
   {:phys {:v [(get-in player [:phys :v 0]) 0]}
    :position [player-x (- floor-y player-h 1)]
    :collision {:grounded? true}}))

(defn control [subj action]
  (let [desired-pose (get-in subj [:person :desired-pose])
        new-subj
        (cond-> subj
          :change-pose
          (person-to-pose (case [action desired-pose]
                            [:up :crawl]  :crouch
                            [:up :crouch] :stand
                            [:up :stand]  :stand
                            [:down :stand]  :crouch
                            [:down :crouch] :crawl
                            [:down :crawl]  :crawl
                            [:crouch :stand] :crouch
                            [:crouch :crouch] :stand
                            [:crouch :crawl] :crouch
                            [:crawl :stand] :crawl
                            [:crawl :crouch] :crawl
                            [:crawl :crawl] :stand))
          (and (= action :up)
               (= desired-pose :stand)
               (get-in subj [:collision :grounded?]))
          (->
           (deep-merge {:collision {:grounded? false}})
           (phys/push-v [0 -5])))]
    new-subj
    ))


(def system
  (fn [{:keys [current-tick time-passed  controls] :as db
        {{{:keys [desired-pose]} :person :as subj} :player} :entities}]
    (let [{:keys [left right trigger]} controls
          firing? (and trigger
                       (> (get-in subj [:weapon :temp :left-rounds]) 0)
                       (< (get-in subj [:weapon :cooldown-ticks]) (- current-tick
                                                                     (get-in subj [:weapon :temp :last-fired-at]))))
          rt (time/relative-time subj time-passed)]
      {:entities (merge (if firing? (weapon/bullet subj) {})
                        {:player (cond-> subj
                                   (and (> left 0.1) (get-in subj [:collision :grounded?]))
                                   ((fn [subj]
                                      (weapon/sway-back 7 10)
                                      (move subj :left rt)))

                                   (and (> right 0.1) (get-in subj [:collision :grounded?]))
                                   (move :right rt)

                                   firing?
                                   (weapon/fire db)
                                   )})
       })))


(defn climb [[a-id a] [obst-id obst]]
  {a-id (player-ground a obst)})

(defmethod collision/collide [:player :floor] [a b] (climb a b))
(defmethod collision/collide [:player :stair] [a b] (climb a b))

(defmethod collision/collide [:enemy :floor] [a b] (climb a b))
(defmethod collision/collide [:enemy :stair] [a b] (climb a b))
(defmethod collision/collide [:enemy :player] [a b] (phys/repel a b))
#_(defmethod collision/collide [:enemy :enemy] [a b]  (phys/repel a b))

