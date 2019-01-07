(ns brawl-haus.fit.weapon
  (:require [brawl-haus.fit.state :as state]
            [brawl-haus.utils :refer [l]]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]))

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
(defn bullet [{[pos-x pos-y] :position
                   }]
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
                        {:angle (l 1 (+ start (/ pressure 1.5) (rand-int (+ (- end start) (* 1.5 pressure)))))
                         :pressure (+ pressure 1)})))


(defn sway-back [_ _]
  #_(swap! state/recoil (fn [old] (+ old
                                   ((if (neg? old) + -)
                                    (+ start (rand-int (- end start))))))))


(defn fire [player {:keys [current-tick]}]
  (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6 7} (take 5 (get-in player [:weapon :temp :played-shot-sounds])))))]
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

(defmulti bullet-hit (fn [subj obj] :body))
(defmethod bullet-hit :body
  [{:keys [health] :as subj} {:keys [position]
                              {[v-x v-y] :v} :phys}]
  (sound/bullet-hit)
  (-> subj
      (health-damage 50)
      (phys/push-v [(/ v-x 3) (/ v-y 5)])
      (assoc-in [:collision :grounded?] false)
      (to-state :stagger)
      ))



(defmethod collision/collide [:bullet :floor]
  [[b-id b] [f-id f]]
  {b-id nil})
(defmethod collision/collide [:bullet :stair]
  [[b-id b] [f-id f]]
  {b-id nil})

(defmethod collision/collide [:bullet :enemy]
  [[b-id b] [e-id e]]
  {b-id nil
   e-id (bullet-hit e b)})
(defmethod collision/collide [:enemy :bullet]
  [ [e-id e] [b-id b]]
  {b-id nil
   e-id (bullet-hit e b)})
