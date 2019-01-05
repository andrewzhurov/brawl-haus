(ns brawl-haus.fit.gravity
  (:require [brawl-haus.fit.time :as time]))


(def gravity-strength 9.8) ;; m/s^2

(def system
  (fn [{:keys [entities time-passed]}]
    {:entities
     (->> entities
          (filter (comp :phys val))
          (map (fn [[id {{[a-x-old a-y-old] :a :keys [m]} :phys
                         {:keys [grounded?]} :collision :as subj}]]
                 (let [rt (time/relative-time subj time-passed)]
                   (if grounded?
                     {id subj}
                     {id (update-in subj [:phys :v] (fn [[old-x old-y]] [(+ old-x
                                                                            (* a-x-old (/ rt 1000)))
                                                                         (+ old-y
                                                                            (* a-y-old (/ rt 1000))
                                                                            (* (* (/ gravity-strength 10) m) (/ rt 1000)))]))}))))
          (apply merge))}))
