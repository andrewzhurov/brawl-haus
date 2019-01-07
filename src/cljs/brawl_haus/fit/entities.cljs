(ns brawl-haus.fit.entities
  (:require [brawl-haus.fit.misc :as misc]
            [brawl-haus.fit.player :as player]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.time :as time]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.weapon :as weapon]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.chase :as chase]
            [brawl-haus.fit.misc :refer [comp-position comp-size]]
            [brawl-haus.fit.events :refer [>evt]]
            [brawl-haus.fit.mnemo :as mnemo]))

(def the-player
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
         (comp-position [500 100])
         (phys/component 5 [0 0] [0 0])
         (collision/component true)
         (chase/component true)))




(def floor
  (merge {:id :floor
          :type :floor}
         {:render {:color "gray"}}
         (comp-position [100 300])
         (comp-size 600 52)
         (collision/component false)))


(def stairs
  (map (fn [stair-num]
         (let [w 15
               h 5]
           (merge {:id (keyword (str "stair-" stair-num))
                   :type :stair}
                  {:render {:color "darkgray"}}
                  (comp-position [(+ 600 (* stair-num w)) (- 300 (* stair-num h))])
                  (comp-size w h)
                  (collision/component false)
                  )))
       (range 1 11)))


(defn enemy [pos]
  (merge {:id (keyword (str (random-uuid)))
          :type :enemy}
         {:body {:health 200
                 :state :normal
                 :frame 0
                 :stagger-time 1000
                 :stun-time 2500}}

         (comp-position pos)
         (comp-size 12 50)
         (collision/component true)
         (phys/component 5 [0 0] [0 0])
         (chase/component true)))

(def enemies [] #_(repeatedly 3 #(enemy [(+ 100 (rand-int 200))  (+ 100 (rand-int 100))])))

