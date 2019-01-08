(ns brawl-haus.fit.weapon
  (:require [brawl-haus.fit.state :as state]
            [brawl-haus.utils :refer [l]]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]
            [brawl-haus.fit.state :as state]
            #_[brawl-haus.fit.events :as events]))

(def ak-47
  {:weight 3.47
   :stabilisation 200; ms/deg
   :muzzle-velocity 715 ;m/s
   :rounds 120 ;30
   :rpm 60
   :cooldown-ticks 3
   :texture "/sandbox/ak-47.svg"})

(defn component [spec]
  {:weapon (merge spec
                  {:temp {:last-fired-at 0
                          :left-rounds (:rounds spec)}})})
(defn rad [deg] (* (/ Math.PI 180) deg))
(defn bot-bullet [[pos-x pos-y :as p1] p2]
  (let [{[disp-x disp-y] :vec-angle
         :keys [angle facing]} (state/calc-angle p1 p2)
        id (keyword (str (random-uuid)))]

    (merge {:id id
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
                (phys/component 0.01
                                [((if (= :left facing) - +)
                                  (* (Math.cos (rad angle)) 13))
                                 (- (* (Math.sin (rad angle)) 13))]
                                [0 0])
                {:self-destruct {:after 3000
                                 :spawn-time (Date.now)}})))



(defn bullet [{[pos-x pos-y] :position
               }
              ]
  (let [{[disp-x disp-y] :vec-angle
         :keys [angle facing]} @state/angle ;; COFX
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
               (phys/component 0.01
                               [((if (= :left facing) - +)
                                 (* (Math.cos (rad angle)) 13))
                                (- (* (Math.sin (rad angle)) 13))]
                               #_[(-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                    (* disp-x)
                                    (* (/ dt 1000)))
                                (-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                    (* disp-y)
                                    (* (/ dt 1000)))]
                               [0 0])
               {:self-destruct {:after 3000
                                :spawn-time (Date.now)}})}))

(defn sway-random [start end]
  (swap! state/recoil (fn [{:keys [pressure angle]}]
                        {:angle (+ start (/ pressure 1.5) (rand-int (+ (- end start) (* 1.5 pressure))))
                         :pressure (+ pressure 1)})))


(defn sway-back [_ _]
  #_(swap! state/recoil (fn [old] (+ old
                                   ((if (neg? old) + -)
                                    (+ start (rand-int (- end start))))))))


(defn fire [player {:keys [current-tick]}]
  (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6} (take 5 (get-in player [:weapon :temp :played-shot-sounds])))))]
    (sound/play-fire next-sound-idx)
    (sway-random 1 2)
    (-> player
        (update-in [:weapon :temp :played-shot-sounds] conj next-sound-idx)
        (update-in [:weapon :temp :left-rounds] dec)
        (assoc-in [:weapon :temp :last-fired-at] current-tick)
        )))

(defn to-state [ent state]
  (-> ent
      (update-in [:body :state]
                 (fn [curr-state]
                   (case [curr-state state]
                     [:normal :normal]   :normal
                     [:normal :stagger]  :stagger
                     [:normal :stun]     :stun
                     [:normal :dead]     :dead

                     [:stagger :normal]  :normal
                     [:stagger :stagger] :stagger
                     [:stagger :stun]    :stun
                     [:stagger :dead]    :stagger-dead

                     [:stun :normal]     :normal
                     [:stun :stagger]    :stun
                     [:stun :stun]       :stun
                     [:stun :dead]       :stun-dead
                     )))
      (assoc-in [:body :state-since] nil)))

(defn health-damage [ent amount] (update-in ent [:body :health] - amount))


(defn bullet-hit
  [subj {:keys [position]
         {[v-x v-y] :v} :phys}
   dmg]
  (let [stateful? (:state (:body subj))]
    (sound/bullet-hit)
    (cond-> (-> subj
                (health-damage dmg)
                (phys/push-v [(/ v-x 3) (/ v-y 5)])
                (assoc-in [:collision :grounded?] false))
      stateful? (to-state :stagger)
      )))



(defmethod collision/collide [:bullet :floor]
  [_ [b-id b] [f-id f]]
  {:entities {b-id nil}})
(defmethod collision/collide [:bullet :stair]
  [_ [b-id b] [f-id f]]
  {:entities {b-id nil}})

(defmethod collision/collide [:bullet :enemy]
  [_ [b-id b] [e-id e]]
  {:entities {b-id nil
              e-id (bullet-hit e b 40)}})

(defmethod collision/collide [:enemy :bullet]
  [_ [e-id e] [b-id b]]
  (let [new-e (bullet-hit e b 40)]
    {:entities {b-id nil
                e-id (if (> 0 (get-in new-e [:body :health]))
                       new-e
                       nil)}}))


(defmethod collision/collide [:bullet :player]
  [db  [b-id b] [p-id p]]
  (let [new-p (health-damage p 25)]
    (if (> 0 (get-in new-p [:body :health]))
      #_(events/add-evt db [:to-level 1])
      {:entities {b-id nil
                  p-id new-p}})))

(defmethod collision/collide [:player :bullet]
  [db [p-id p] [b-id b]]
  (let [new-p (bullet-hit p b 25)]
    (if (> 0 (get-in new-p [:body :health]))
      (.play (js/Audio "/sandbox/07_loss.wav"))
      #_(events/add-evt db [:to-level 1])
      {:entities {b-id nil
                  p-id new-p}})))
