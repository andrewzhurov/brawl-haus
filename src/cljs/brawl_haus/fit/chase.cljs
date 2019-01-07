(ns brawl-haus.fit.chase
  (:require [brawl-haus.fit.time :as time]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.utils :refer [l]]
            [brawl-haus.fit.weapon :as weapon]))

(defn component [active?]
  {:chase {:active? active?}})

(defn brace [{{:keys [state state-since stagger-time stun-time]} :body :as e} time-passed-all]
  (if (> time-passed-all (+ state-since (case state :stun stun-time :stagger stagger-time)))
    (weapon/to-state e :normal)
    e))

(def system
  (fn [{:keys [time-passed-all] :as db}]
    (let [p (get-in db [:entities :player])
          es (filter (comp #{:enemy} :type val) (:entities db))]
      {:entities (reduce (fn [acc [e-id {{:keys [state state-since]} :body :as e}]]
                           (assoc acc e-id
                                  (let [[disp-x _] (u/displacement (:position p) (:position e))
                                        rt (time/relative-time e (:time-passed db))
                                        e (if (nil? state-since)
                                            (assoc-in e [:body :state-since] time-passed-all)
                                            e)]
                                    (case state
                                      :normal (if (neg? disp-x)
                                                (phys/push-v e [6 0] 4 rt)
                                                (phys/push-v e [-6 0] 4 rt))
                                      :stagger (brace e time-passed-all)
                                      :stun    (brace e time-passed-all)

                                      e)
                                    )))
                         {}
                         es)}
      )))


