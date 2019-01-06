(ns brawl-haus.fit.weapon
  (:require [brawl-haus.fit.state :as state]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]))

(def ak-47
  {:weight 3.47
   :muzzle-velocity 715 ;m/s
   :rounds 30
   :rpm 60
   :cooldown-ticks 30
   :texture "/sandbox/ak-47.svg"})

(defn component [spec]
  {:weapon (merge spec
                  {:temp {:last-fired-at 0
                          :left-rounds 30}})})

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

(defn fire [player {:keys [current-tick]}]
  (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6 7} (take 5 (get-in player [:weapon :temp :played-shot-sounds])))))]
    (sound/play-fire next-sound-idx)
    (-> player
        (update-in [:weapon :temp :played-shot-sounds] conj next-sound-idx)
        (update-in [:weapon :temp :left-rounds] dec)
        (assoc-in [:weapon :temp :last-fired-at] current-tick)
        (update-in [:weapon :temp :inacuracy] + (inc (rand-int 3))))))



(defmethod collision/collide [:bullet :floor]
  [[b-id b] [f-id f]]
  {b-id nil})
(defmethod collision/collide [:bullet :stair]
  [[b-id b] [f-id f]]
  {b-id nil})

(defmethod collision/collide [:bullet :enemy]
  [[b-id b] [e-id e]]
  {b-id nil
   e-id nil})
(defmethod collision/collide [:enemy :bullet]
  [ [e-id e] [b-id b]]
  {b-id nil
   e-id nil})
