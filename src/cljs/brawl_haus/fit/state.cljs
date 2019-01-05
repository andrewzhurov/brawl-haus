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

(def init-db {:evt-history {}
              :current-tick 0
              ;:now (Date.now)
              :prev-mouse [0 0]
              :mouse [0 0]
              :controls init-controls
              :entities {}})

(def db (r/atom init-db))
(defn init! [] (reset! db init-db))


(def mouse (r/cursor db [:mouse]))
(def ppos (r/cursor db [:entities :player :position]))
(def angle (ra/reaction
            (let [[pos-x pos-y] @ppos
                  angle (u/calc-angle [pos-x (+ pos-y 7)] @mouse)
                  facing (if (and (<= angle 90)
                                  (>= angle -90))
                           :left :right)
                  angle (if (= :right facing)
                          (- (- angle 180))
                          angle)]
              {:vec-angle (u/displacement [(+ pos-x 7) pos-y] @mouse)
               :angle angle
               :facing facing})))
