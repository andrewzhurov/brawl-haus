(ns brawl-haus.fit.entities
  (:require [brawl-haus.fit.misc :as misc]
            [brawl-haus.fit.player :as player]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.time :as time]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.weapon :as weapon]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.chase :as chase]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]
            [brawl-haus.fit.mnemo :as mnemo]
            [brawl-haus.utils :refer [l]]))

(defn add-evt [{:keys [current-tick] :as db} evt]
  (l "EVT:" evt)
  (update-in db [:evt-history (inc current-tick)] u/conjv evt))



(defn the-player [pos]
  (merge {:id :player
          :type :player
          :controlled true}
         {:body {:health 200
                 ;:state :normal
                 ;:frame 0
                 }}

         {:render {:color "orange"
                   :facing :left}}
         (player/comp-person {:poses {:stand {:height 40
                                              :width 10
                                              :front-speed 6
                                              :front-max 4
                                              :back-speed 5
                                              :back-max 3
                                              }
                                      :crouch {:height 20
                                               :width 13
                                               :front-speed 5
                                               :front-max 3
                                               :back-speed 4
                                               :back-max 2.5}
                                      :crawl {:height 10
                                              :width 40
                                              :front-speed 3
                                              :front-max 1
                                              :back-speed 3
                                              :back-max 0.5}}
                              :weight 70})
         (weapon/component weapon/ak-47)
         (comp-position pos)
         (phys/component 5 [0 0] [0 0])
         (collision/component true)
         (chase/component true)))




(defn floor [pos w h]
  (merge {:id (keyword (str "floor--" (random-uuid)))
          :type :floor}
         {:render {:color "burlywood"}}
         (comp-position pos)
         (comp-size w h)
         (collision/component false)))


(defn stairs [[pos-x pos-y] w h steps]
  (map (fn [stair-num]
         (merge {:id (keyword (str "stair-" stair-num "--" (random-uuid)))
                 :type :stair}
                {:render {:color "darkgray"}}
                (comp-position [(+ pos-x (* stair-num w)) (- pos-y (* stair-num h))])
                (comp-size w h)
                (collision/component false)
                ))
       (range 0 steps)))

(defn portal [pos level]
  (merge {:id (keyword (str "next-level-entrance--" (random-uuid)))
          :type :next-level-entrance
          :render {:color "purple"}
          :portal {:level level}}
         (comp-position pos)
         (comp-size 20 70)
         (collision/component false)))
(defmethod collision/collide [:player :next-level-entrance]
  [{:keys [current-tick] current-level :level} _ [_ {{:keys [level]} :portal}]]
  (if (not= current-level level)
    {:evt-history {(inc current-tick) [[:to-level level]]}}
    {})
  )


(defn enemy [pos range]
  (merge {:id (keyword (str (random-uuid)))
          :type :enemy}
         {:watcher {:at nil
                    :spot-distance range
                    :cooldown 1000
                    :last-fired-at 0}}
         {:body {:health 200
                 :state :normal
                 :frame 0
                 :stagger-time 1000
                 :stun-time 2500}}

         (comp-position pos)
         (comp-size 12 50)
         (collision/component true)
         (phys/component 5 [0 0] [0 0])
         #_(chase/component true)))


(defn rand-in [start end] (+ start (rand-int (- end start))))
(defn enemies [x] (repeatedly x #(enemy [(rand-in 300 800)  150] 150)))



(defn the-zone [lv]
  (->
   [(floor [0 400] 850 50)
    (the-player [30 280])
    (portal [750 370] (inc lv))
    (floor [74 372] 50 10)
    (floor [175 332] 50 10)
    (floor [229 308] 16 40)

    (floor [309 362] 150 20)

    (floor [362 286] 16 40)
    (floor [376 303] 90 20)
    (floor [516 345] 90 15)
    (floor [580 330] 25 30)
    (floor [693 310] 20 30)
    (floor [711 330] 60 10)

    (enemy [383 250] 340)
    (enemy [714 289] 300)]

   (into (stairs [112 367] 13 8 5))
   (into (stairs [285 388] 13 8 2))
   (into (enemies lv))))
