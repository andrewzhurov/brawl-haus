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
         {:render {:color "orange"
                   :facing :left}}
         (player/comp-person {:poses {:stand {:height 40
                                              :width 10
                                              :front-speed 4
                                              :front-max 2
                                              :back-speed 3
                                              :back-max 1
                                              }
                                      :crouch {:height 20
                                               :width 13
                                               :front-speed 4
                                               :front-max 1.25
                                               :back-speed 1.5
                                               :back-max 0.75}
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


(def enemy
  (merge {:id :enemy
          :type :enemy}
         (comp-position [100 100])
         (comp-size 7 15)
         (collision/component false)
         (phys/component 5 [0 0] [0 0])
         (chase/component true)))
