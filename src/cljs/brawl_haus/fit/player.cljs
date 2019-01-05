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
    (phys/push-v ent [(* 6 (first speed)) (* 3 (second speed))] (* max 3) dt)))

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
           (phys/push-v [0 -3])))]
    new-subj
    ))


(defn bullet [{[pos-x pos-y] :position
                   }]
  (let [{[disp-x disp-y] :vec-angle
         :keys [angle]} @state/angle ;; COFX
        id (keyword (str (random-uuid)))
        dt 16] ;; LIE
    {id (merge {:id id
                :type :bullet}
               {:render {:color "steel"
                         :angle angle}}
               (comp-position [(-> (/ 40 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                   (* disp-x)
                                   (+ pos-x))
                               (-> (/ 40 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                   (* disp-y)
                                   (+ pos-y 6))])
               (comp-size 4 2)
               (collision/component true)
               (phys/component 0.1
                               [(-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                    (* disp-x)
                                    (* (/ dt 1000)))
                                (-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                    (* disp-y)
                                    (* (/ dt 1000)))]
                               [0 0])
               {:self-destruct {:after 20000
                                :spawn-time (Date.now)}})}))

(def system
  (fn [{:keys [current-tick time-passed  controls] :as db
        {{{:keys [desired-pose]} :person :as subj} :player} :entities}]
    (let [{:keys [left right trigger]} controls
          firing? (and trigger
                       (> (get-in subj [:weapon :temp :left-rounds]) 0)
                       (< (get-in subj [:weapon :cooldown-ticks]) (- current-tick
                                                                     (get-in subj [:weapon :temp :last-fired-at]))))
          rt (time/relative-time subj time-passed)]
      {:entities (cond-> {:player (cond-> subj
                                    (and (> left 0.1) (get-in subj [:collision :grounded?]))
                                    (move :left rt)

                                    (and (> right 0.1) (get-in subj [:collision :grounded?]))
                                    (move :right rt)

                                    firing?
                                    ((fn [player]
                                       (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6 7} (take 5 (get-in subj [:weapon :temp :played-shot-sounds])))))]
                                         (sound/play-fire next-sound-idx)
                                         (-> player
                                             (update-in [:weapon :temp :played-shot-sounds] conj next-sound-idx)
                                             (update-in [:weapon :temp :left-rounds] dec)
                                             (assoc-in [:weapon :temp :last-fired-at] current-tick)))))

                                    (and (<= left 0.1) (<= right 0.1))
                                    (phys/throttle rt))}
                   firing? (merge (bullet subj)))
       })))




(defmethod collision/collide [:player :floor]
  [[p-id player] [f-id floor]]
  {p-id (player-ground player floor)})

(defmethod collision/collide [:player :stair]
  [[p-id {[w h] :size
          [player-x _] :position :as player}]
   {[_ stair-y] :position :as stair}]
  {p-id (player-ground player stair)})
