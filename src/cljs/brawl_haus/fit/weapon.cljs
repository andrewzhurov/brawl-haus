(ns brawl-haus.fit.weapon
  (:require [brawl-haus.fit.state :as state]))

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
