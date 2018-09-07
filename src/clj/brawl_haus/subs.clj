(ns brawl-haus.subs
  (:require [re-frame.core :as rf]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            ))

(defn l [desc expr] (println desc expr) expr)

(defn location [state conn-id]
  (get-in state [:users conn-id :location]))

(defn user [state conn-id]
  (get-in state [:users conn-id]))

(defn race-progress* [state race-id]
  (let [{:keys [participants race-text]} (get-in state [:open-races race-id])]
    (for [[conn-id {:keys [speed left-chars]}] participants]
      (let [{:keys [nick location]} (user state conn-id)]
        {:nick nick
         :speed speed
         :progress (if left-chars
                     (- 100
                        (-> left-chars
                            (/ (count race-text))
                            (* 100)))
                     0)
         :did-finish (= 0 left-chars)
         :did-quit (= (:location-id location) :quit)}))))


(defn current-race [state conn-id]
  (let [race-id (->> (location state conn-id)
                     :params
                     :race-id)]
    (get-in state [:open-races race-id])))

(rf/reg-sub
 :countdown
 (fn [db [_ conn-id]]
   (select-keys (l "CURR:"(current-race db conn-id)) [:starts-at])))

(rf/reg-sub
 :text-race
 (fn [db [_ conn-id]]
   (let [race (current-race db conn-id)]
     {:has-not-began (empty? (:race-text race))
      :race-text (:race-text race)
      :starts-at (:starts-at race)})))

(rf/reg-sub
 :race-progress
 (fn [db [_ conn-id]]
   (->> (location db conn-id)
        :params
        :race-id
        (race-progress* db))))


;; Space versus
(defn calc-readiness [charge-time charging-since]
  (let [percentage (when charging-since
                     (-> (t/interval charging-since (t/now))
                         (t/in-millis)
                         (/ charge-time)
                         (* 100)
                         (double)))]
    (cond
      (not charging-since) {:is-on false
                            :percentage 0
                            :is-ready false}
      (< percentage 100) {:is-on true
                          :percentage percentage
                          :is-ready false}
      :ready {:is-on true
              :percentage 100
              :is-ready true})))

(defn ship [db ship-id]
  (get-in db [:games :sv :ship ship-id]))

(defn power-hub [db ship-id]
  (:power-hub (ship db ship-id)))
(defn systems [db ship-id]
  (:systems (ship db ship-id)))
(defn system [db ship-id system-id]
  (get (systems db ship-id) system-id))

(defn power-in-use [db ship-id]
  (let [systems (systems db ship-id)]
    (reduce (fn [acc [_ system]] (+ acc (:in-use system))) 0 systems)))

(defn left-power [db ship-id]
  (- (:generating (power-hub db ship-id)) (power-in-use db ship-id)))

(defn weapon [db ship-id stuff-id]
  (get-in (systems db ship-id) [:weapons :stuff stuff-id]))

(defn weapon-readiness [db ship-id stuff-id]
  (let [{:keys [slot charge-time charging-since is-selected damage id]} (weapon db ship-id stuff-id)]
    (assoc (calc-readiness charge-time charging-since)
           :slot slot
           :is-selected is-selected
           :damage damage
           :id id)))


(defn weapons-readiness [db ship-id]
  (map #(weapon-readiness db ship-id %) (keys (:stuff (system db ship-id :weapons)))))

(defn locations [db]
  (->> (:users db)
       vals
       (map :location)
       (filter #(= :space-versus (:location-id %)))
       (map #(get-in % [:params :ship-location-id]))
       set))

(defn ships-at [db location-id]
  (->> (:users db)
       (filter (fn [[id user]] (and (= :space-versus (get-in user [:location :location-id]))
                                    (= location-id (get-in user [:location :params :ship-location-id])))))
       (map key)
       set))

(def subs
  {
   :set-nick
   (fn [db _ conn-id]
     {:current-nick (get-in db [:users conn-id :nick])})

   :self
   (fn [db _ conn-id]
     (select-keys (get-in db [:users conn-id]) [:conn-id :nick]))

   :participants
   (fn [db _ _]
     (->> (:users db)
          vals
          (remove #(= :quit (get-in % [:location :location-id])))
          (map :nick)
          (sort)))


   :messages
   (fn [db _ conn-id]
     {:is-empty (= 0 (count (:messages db)))
      :messages (->> (:messages db)
                     (sort-by :received-at)
                     (reverse)
                     (map (fn [{:keys [id sender text received-at]}]
                            {:id id
                             :nick (:nick (user db sender))
                             :is-my (= (:nick (user db sender)) (:nick (user db conn-id)))
                             :text text
                             :received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))})))})

   :personal-info
   (fn [db _ conn-id]
     (dissoc (get-in db [:users conn-id]) :conn))

   :location
   (fn [db _ conn-id]
     (location db conn-id))

   :sv/systems
   (fn [db _ conn-id]
     (get-in db [:games :sv :ship conn-id :systems]))

   :sv.ship/integrity
   (fn [db _ conn-id]
     {:integrity (:integrity (ship db conn-id))})

   :sv.weapon/readiness
   (fn [db [_ stuff-id] conn-id]
     (weapon-readiness db conn-id stuff-id))

   :sv.weapons/readiness
   (fn [db [_ ship-id] conn-id]
     (weapons-readiness db (or ship-id conn-id)))

   :sv.power/info
   (fn [db _ conn-id]
     {:max (:max (power-hub db conn-id))
      :in-use (power-in-use db conn-id)
      :left (left-power db conn-id)})

   :view.sv/systems
   (fn [db [_ ship-id] conn-id]
     (map (fn [[system-id {:keys [max damaged in-use]}]]
            {:id system-id
             :damaged damaged
             :idle (-> max (- damaged) (- in-use))
             :in-use in-use})
          (systems db (or ship-id conn-id))))

   :sv.weapons.stuff/view
   (fn [db _ conn-id]
     (map (fn [[stuff-id a-stuff]]
            (assoc (select-keys a-stuff [:name :required-power])
                   :stuff-id stuff-id))
          (:stuff (system db conn-id :weapons))))

   :sv/enemies
   (fn [db _ conn-id]
     (keys (dissoc (get-in db [:games :sv :ship]) conn-id)))

   :sv/locations
   (fn [db [_ location-id] _]
     (->> (:users db)
          vals
          (map :location)
          (filter #(= :space-versus (:location-id %)))
          (map #(get-in % [:params :ship-location-id]))
          set))

   :sv.location/ships
   (fn [db [_ location-id] _]
     (ships-at location-id))

   :view.sv/locations
   (fn [db _ _]
     (for [location-id (locations db)]
       {:location-id location-id
        :location-name (apply str (take 5 (name location-id)))
        :ship-captains (map (fn [ship-id] (get-in db [:users ship-id :nick]))
                            (ships-at db location-id))}))

   :view.sv/ship
   (fn [db [_ ship-id] _]
     {:ship-id ship-id
      :systems (map (fn [[system-id {:keys [max damaged in-use]}]]
                      {:id system-id
                       :status (cond (= 0 damaged) "full"
                                     (not= max damaged) "damaged"
                                     :else "crit")})
                    (systems db ship-id))
      :weapons (weapons-readiness db ship-id)})

   :view.sv/ships
   (fn [db [_ location-id] _]
     (for [ship-id (ships-at db location-id)]
       {:ship-id ship-id
        :systems (map (fn [[system-id {:keys [max damaged in-use]}]]
                        {:id system-id
                         :status (cond (= 0 damaged) "integrity-full"
                                       (not= max damaged) "integrity-damaged"
                                       :else "integrity-wrecked")})
                      (systems db ship-id))
        :weapons (weapons-readiness db ship-id)}))

   :sv.ship/nick
   (fn [db [_ ship-id] _]
     {:nick (get-in db [:users ship-id :nick])})

   :sv.shield/readiness
   (fn [db _ conn-id]
     (let [{:keys [charge-time powered-since]} (system db conn-id :shields)]
       (calc-readiness charge-time (first powered-since))))})

(defn derive [db [sub-id :as sub] & params]
  (if-let [sub-fn (get subs sub-id)]
    (do #_(println "derive: " (pr-str sub) " " (pr-str params))
        (apply sub-fn db sub params))
    (println "!!No sub found:" (pr-str sub))))
