(ns brawl-haus.subs
  (:require [re-frame.core :as rf]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clojure.set]
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





;; Space versus
(defn calc-readiness [{:keys [charge-time charging-since fire-time firing-since is-selected]}]
  (let [now (t/now)
        is-firing (when firing-since
                    (-> (t/interval firing-since now)
                        (t/in-millis)
                        (< fire-time)))
        percentage (when (and (not is-firing) charging-since)
                     (-> (t/interval (or firing-since charging-since) now)
                         (t/in-millis)
                         (/ charge-time)
                         (* 100)
                         (double)))]
    (cond
      (not charging-since) {:status :idle
                            :percentage 0}
      is-firing {:status :firing
                 :percentage 0}
      is-selected {:status :selected
                   :percentage 100}
      (< percentage 100) {:status :charging
                          :percentage percentage}
      :ready {:status :ready
              :percentage 100})))

(defn ships [db]
  (get-in db [:games :sv :ship]))
(defn ship [db ship-id]
  (get (ships db) ship-id))

(defn ship-wrecked? [db ship-id]
  (every? (fn [[_ {:keys [max damaged]}]] (= max damaged)) (:systems (ship db ship-id))))

(defn power-hub [db ship-id]
  (:power-hub (ship db ship-id)))
(defn systems [db ship-id]
  (:systems (ship db ship-id)))

(defn system [db ship-id system-id]
  (-> (ship db ship-id)
      :systems
      (get system-id)))

(defn power-in-use [db ship-id]
  (->> (systems db ship-id)
       vals
       (filter :in-use)
       (map :in-use)
       (reduce +)))

(defn left-power [db ship-id]
  (if-let [generating (:generating (power-hub db ship-id))]
    (- generating (power-in-use db ship-id))
    0))

(defn weapon [db ship-id stuff-id]
  (get-in (systems db ship-id) [:weapons :stuff stuff-id]))

(defn weapon-readiness [db ship-id stuff-id]
  (let [a-weapon (weapon db ship-id stuff-id)]
    (merge (select-keys a-weapon [:id :slot :damage])
           (calc-readiness  a-weapon))))

(defn shield-readiness [db ship-id]
  (let [{:keys [charge-time powered-since last-absorption]} (system db ship-id :shields)]
    (calc-readiness {:charge-time charge-time
                     :charging-since (if last-absorption
                                       (t/max-date last-absorption (first powered-since))
                                       (first powered-since))})))


(defn weapons-readiness [db ship-id]
  (map #(weapon-readiness db ship-id %) (keys (:stuff (system db ship-id :weapons)))))

(defn locations [db]
  (->> (:users db)
       vals
       (map :location)
       (filter #(= :space-versus (:location-id %)))
       (map #(get-in % [:params :ship-location-id]))
       set))


(declare derive)
(def subs
  {:my-subs
   (fn [db _ conn-id]
     (->> (:subs db)
          (filter (fn [[_ subber]] (= subber conn-id)))
          (map first)))

   ;; Race
   :countdown
   (fn [db _ conn-id]
     (select-keys (current-race db conn-id) [:starts-at]))

   :text-race
   (fn [db _ conn-id]
     (let [race (current-race db conn-id)]
       {:has-not-began (empty? (:race-text race))
        :race-text (:race-text race)
        :starts-at (:starts-at race)}))

   :race-progress
   (fn [db _ conn-id]
     (->> (location db conn-id)
          :params
          :race-id
          (race-progress* db)))


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

   ;; Space Versus
   :sv.ship/name
   (fn [db [_ ship-id] _]
     (get-in db [:users ship-id :nick]))

   :sv/systems
   (fn [db _ conn-id]
     (get-in db [:games :sv :ship conn-id :systems]))

   :sv.ship/systems
   (fn [db [_ ship-id] _]
     (keys (get-in (ship db ship-id) [:systems])))

   :sv.ship/weapons
   (fn [db [_ ship-id] _]
     (keys (get-in (ship db ship-id) [:systems :weapons :stuff])))

   :sv.weapon/view
   (fn [db [_ ship-id weapon-id] _]
     (-> (get-in (system db ship-id :weapons) [:stuff weapon-id])
         (select-keys [:id :name :required-power])))

   :sv.weapon/readiness
   (fn [db [_ stuff-id] conn-id]
     (weapon-readiness db conn-id stuff-id))

   :sv.weapon/obscured-readiness
   (fn [db [_ ship-id weapon-id] _]
     (select-keys (weapon-readiness db ship-id weapon-id) [:slot :status]))

   :sv.weapons/readiness
   (fn [db [_ ship-id] conn-id]
     (weapons-readiness db (or ship-id conn-id)))

   :sv.shield/readiness
   (fn [db [_ ship-id] conn-id]
     (shield-readiness db (or ship-id conn-id)))

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


   :sv/locations
   (fn [db _ _]
     (let [static-locations (->> (get-in db [:games :sv :locations])
                                 keys
                                 set)
           dynamic-locations (->> (ships db)
                                  (map (comp :location-id val))
                                  (remove nil?)
                                  set)]
       (clojure.set/union static-locations
                          dynamic-locations)))

   :sv.location/details
   (fn [db [_ location-id] _]
     (reduce (fn [location [ship-id ship]]
               (cond-> (merge location
                              {:location-id location-id
                               :location-name (apply str (take 5 (name (or location-id "VOID"))))})
                 (= location-id (:location-id ship))
                 (update :ships (comp set conj) ship-id)))
             (get-in db [:games :sv :locations location-id])
             (ships db)))

   :sv.current-location/details
   (fn [db _ conn-id]
     (let [current-location-id (:location-id (ship db conn-id))]
       (update (derive db [:sv.location/details current-location-id] nil)
               :ships disj conn-id)))

   :sv.system/status
   (fn [db [_ ship-id system-id] _]
     (let [{:keys [max damaged last-hit-at]} (get-in (ship db ship-id) [:systems system-id])]
       {:status (cond
                  (zero? damaged) "integrity-full"
                  (not= damaged max) "integrity-damaged"
                  :else "integrity-wrecked")
        :last-hit-at_FOR_UPDATE_SEND (str last-hit-at)}))

   :sv.cargo/scrap
   (fn [db _ conn-id]
     (get-in db [:games :sv :ship conn-id :cargo :scrap]))

   :sv.ship/wrecked?
   (fn [db [_ ship-id] _]
     (every?
      (fn [[_ {:keys [max damaged]}]]
        (= max damaged))
      (:systems (ship db ship-id))))})

(defn derive [db [sub-id :as sub] & params]
  (if-let [sub-fn (get subs sub-id)]
    (do #_(println "derive: " (pr-str sub) " " (pr-str params))
        (apply sub-fn db sub params))
    (println "!!No sub found:" (pr-str sub))))
