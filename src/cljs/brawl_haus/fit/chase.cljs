(ns brawl-haus.fit.chase
  (:require [brawl-haus.fit.time :as time]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.utils :refer [l]]))

(defn component [active?]
  {:chase {:active? active?}})

(def system
  (fn [db]
    (let [p (get-in db [:entities :player])
          es (filter (comp #{:enemy} :type val) (:entities db))]
      {:entities (reduce (fn [acc [e-id e]]
                           (let [[disp-x _] (u/displacement (:position p) (:position e))
                                 rt (time/relative-time e (:time-passed db))]
                             (assoc acc e-id (if (neg? disp-x)
                                               (phys/push-v e [6 0] 4 rt)
                                               (phys/push-v e [-6 0] 4 rt)))))
                         {}
                         es)}
      )))


