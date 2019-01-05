(ns brawl-haus.fit.utils
  (:require [clojure.data :refer [diff]]))


(defn conjv [v? val] (if v? (conj v? val) [val]))

(defn update-set [coll match update-fn]
  (into #{} (map (fn [el]
                   (let [[a b ab] (diff el match)]
                     (if ab
                       (update-fn el)
                       el)))
                 coll)))

(defn look-up [coll match]
  (some #(let [[a b ab] (diff % match)]
           (when ab %))
        coll))

(defn inspect [data x y]
  [:g
   (cond
     (map? data)
     (map-indexed (fn [idx [k v]]
                    ^{:key idx}
                    [:g
                     [:text {:x x
                             :y (+ y (* 12 (inc idx)))
                             :color :gray}
                      (pr-str k v)]])
                  data)
     :default [:text {:x x :y y :color :orange} (pr-str data)])])

(defn coords [e] [(.-clientX e) (.-clientY e)])

(defn displacement [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])
(defn calc-angle [[x1 y1] [x2 y2]]
  (* (js/Math.atan2 (- y1 y2) (- x1 x2)) (/ 180 js/Math.PI)))
