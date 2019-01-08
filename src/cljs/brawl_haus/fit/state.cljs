(ns brawl-haus.fit.state
  (:require [brawl-haus.fit.utils :as u]
            [reagent.core :as r]
            [reagent.ratom :as ra]))

(def init-controls {:up 0
                    :down 0
                    :left 0
                    :right 0
                    :crouch 0
                    :crawl 0
                    :trigger false})

(def init-db {:level nil
              :evt-history {1 [[:to-level 1]]}
              :current-tick 0
              ;:now (Date.now)
              :prev-mouse [0 0]
              :mouse [0 0]
              :controls init-controls
              :entities {}})

(def db (r/atom init-db))
(defn init! [] (reset! db init-db))



(defn rad [deg] (* (/ Math.PI 180) deg))
(def mouse (r/cursor db [:mouse]))
(def ppos (r/cursor db [:entities :player :position]))
(def recoil (r/atom {:pressure 0.1
                     :angle 0.1})) ;deg
(defn calc-angle [[pos-x pos-y] p2]
  (let [angle (u/calc-angle [pos-x (+ pos-y 7)] p2)
        facing (if (and (<= angle 90)
                        (>= angle -90))
                 :left :right)
        angle (+ 0 (if (= :right facing)
                     (- (- angle 180))
                     angle))
        tang (Math.tan (* angle (/ Math.PI 180)))]
    {:vec-angle (u/displacement [(+ pos-x 2) (+ pos-y 10)] p2)

     :angle angle
     :facing facing}))


(def angle (ra/reaction
            (let [[pos-x pos-y] @ppos
                  angle (u/calc-angle [pos-x (+ pos-y 7)] @mouse)
                  facing (if (and (<= angle 90)
                                  (>= angle -90))
                           :left :right)
                  angle (+ (:angle @recoil) (if (= :right facing)
                                     (- (- angle 180))
                                     angle))
                  tang (Math.tan (* angle (/ Math.PI 180)))]
              {:vec-angle (u/displacement [(+ pos-x 2) (+ pos-y 10)] @mouse)

               :angle angle
               :facing facing})))
