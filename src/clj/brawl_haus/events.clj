(ns brawl-haus.events
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [re-frame.events]
            [re-frame.cofx]
            [re-frame.fx]
            [re-frame.std-interceptors]
            [brawl-haus.data :as data]
            [re-frame.core :as rf]
            [brawl-haus.subs :as subs]
            ))

(defn l [desc expr] (println desc expr) expr)

(defn location [state conn-id]
  (get-in state [:users conn-id :location]))
(defn navigate [state conn-id location]
  (assoc-in state [:users conn-id :location] location))

(defn gen-uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))

(def init-db {:open-races {}
              :messages #{}
              :subs #{}
              :games {:sv {:locations {:station-location {:station {}}}}}})

(def db (atom init-db))

(rf/reg-event-db
 :race/ready-set-go
 (fn [db [_ _ race-id]]
   (update-in db [:open-races race-id]
              merge {:status :began
                     :race-text (rand-nth data/long-texts)})))

(defn calc-speed [text start finish]
  (let [minutes (/ (t/in-seconds (t/interval (c/from-date start)
                                             (c/from-date finish)))
                   60)
        chars (count text)]
    (int (/ chars minutes))))

(defn race-to-be [db]
  (let [[[_ race]] (filter (fn [[_ {:keys [status]}]] (= :to-be status))
                           (get db :open-races))]
    race))

(defn enter-race [db race-id conn-id]
  (-> db
      (assoc-in [:open-races race-id :participants conn-id] {})
      (navigate conn-id {:location-id :race-panel
                         :params {:race-id race-id}})))

(defn ensure-race [db conn-id]
  (if-not (race-to-be db)
    (let [new-race {:id (gen-uuid)
                    :participants {}
                    :status :to-be
                    :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date))}]
      (assoc-in db [:open-races (:id new-race)] new-race))
    db))

(rf/reg-event-fx
 :race/attend
 (fn [{:keys [db]} [_ conn-id]]
   (let [db-with-race (ensure-race db conn-id)]
     {:db (enter-race db-with-race (:id (race-to-be db-with-race)) conn-id)
      :later-evt {:ms 7000 :evt [:race/ready-set-go nil (:id (race-to-be db-with-race))]}
      })))

(rf/reg-fx
 :later-evt
 (fn [{:keys [ms evt] :as in}]
   (l "later evt: " in)
   (Thread/sleep ms)
   (rf/dispatch evt)))

(rf/reg-event-db
 :race/left-text
 (fn [db [_ conn-id left-text]]
   (let [is-finished (zero? (count left-text))
         race-id (-> (location db conn-id) :params :race-id)
         {:keys [race-text starts-at]} (get-in db [:open-races race-id])]
     (assoc-in db
               [:open-races race-id :participants conn-id]
               {:left-chars (count left-text)
                :speed (calc-speed race-text starts-at (now))}))))


(rf/reg-event-db
 :hiccup-touch/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :hiccup-touch})))



(rf/reg-event-db
 :ccc/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :ccc-panel})))



;; Space versus

(defn deep-merge [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(def ship-basic-power-hub
  {:power-hub {:max 7
               :generating 5}})

(def ship-basic-shields
  {:systems {:shields {:max 4
                       :damaged 0
                       :in-use 0
                       :charge-time 3000
                       :powered-since []}}})

(def ship-basic-engines
  {:systems {:engines {:max 3
                       :damaged 0
                       :in-use 0
                       :powered-since []}}})

(def ship-basic-weapons
  {:systems {:weapons {:max 3
                       :damaged 0
                       :in-use 0
                       :powered-since []}}})

(def ship-burst-laser-2
  {:systems {:weapons {:stuff {:burst-laser-2 {:id :burst-laser-2
                                               :slot 1
                                               :name "Burst Laser II"
                                               :required-power 2
                                               :damage 2
                                               :charge-time 2000
                                               :fire-time 500
                                               :charging-since nil
                                               :firing-since nil
                                               :is-selected false}}}}})

(def weapon-basic-laser
  {:id :basic-laser
   :slot 2
   :name "Basic Laser"
   :required-power 1
   :damage 1
   :charge-time 3333
   :fire-time 333
   :charging-since nil
   :firing-since nil
   :is-selected false})

(def ship-basic-laser
  {:systems {:weapons {:stuff {:basic-laser weapon-basic-laser}}}})

(def basic-ship
  (deep-merge
   ship-basic-power-hub
   ship-basic-shields
   ship-basic-engines
   ship-basic-weapons
   ship-basic-laser
   ))



(defn sv [db]
  (get-in db [:games :sv]))

(defn deplete-power [system]
  (let [rational-in-use (max (min (- (:max system) (:damaged system)) (:in-use system)) 0)]
    (-> system
        (assoc :in-use rational-in-use)
        (assoc :powered-since (vec (take rational-in-use (:powered-since system)))))))

(defn turn-off-weapons [db ship-id]
  (if (get-in db [:games :sv :ship ship-id :systems :weapons])
    (update-in db [:games :sv :ship ship-id :systems :weapons :stuff]
               (fn [stuff]
                 (into {} (map (fn [[id a-stuff]]
                                 [id (merge a-stuff
                                            {:charging-since nil
                                             :firing-since nil
                                             :is-selected false})]))
                       stuff)))
    db))

(defn ensure [struct path val]
  (update-in struct path #(if (nil? %) val %)))

(declare drive)
(def events
  (atom
   {:drop-my-subs
    (fn [db _ conn-id]
      (update db :subs #(set (remove (fn [_ subber] (= subber conn-id)) %))))

    :chat/add-message
    (fn [db [_ text] conn-id]
      (update db :messages conj {:text text
                                 :id (gen-uuid)
                                 :sender conn-id
                                 :received-at (now)}))

    :chat/set-nick
    (fn [db [_ nick] conn-id]
      (assoc-in db [:users conn-id :nick] nick))

    :subscribe
    (fn [db [_ sub] conn-id]
      (update db :subs (fn [old] (conj (set old) [sub conn-id]))))

    :unsubscribe
    (fn [db [_ sub] conn-id]
      (update db :subs disj [sub conn-id]))

    :conn/on-create
    (fn [db [_ {:keys [conn-id conn]}]]
      (let [anonymous-user {:nick (rand-nth data/names)
                            :conn-id conn-id
                            :conn conn}]
        (-> db
            (assoc-in [:users conn-id] anonymous-user)
            (navigate conn-id {:location-id :home-panel}))))

    :conn/on-close
    (fn [db [_ conn-id]]
      (-> db
          (navigate conn-id {:location-id :quit})
          (update-in [:users conn-id] dissoc :conn)))

    :home/attend
    (fn [db _ conn-id]
      (navigate db conn-id {:location-id :home-panel}))

    :sv/attend
    (fn [db [_ {:keys [with-ship]}] conn-id]
      (-> db
          (navigate conn-id {:location-id :space-versus})
          (ensure [:games :sv :ship conn-id] (merge (or with-ship basic-ship)
                                                    {:ship-id conn-id}))
          (drive [:sv/jump (str (gen-uuid))] conn-id)))

    :sv.system/power-up
    (fn [db [_ system-id] conn-id]
      (let [left-power (subs/left-power db conn-id)
            {:keys [max damaged in-use]} (subs/system db conn-id system-id)]
        (if (and (> left-power 0)
                 (> (- (- max damaged) in-use) 0))
          (-> db
              (update-in [:games :sv :ship conn-id :systems system-id :in-use] inc)
              (update-in [:games :sv :ship conn-id :systems system-id :powered-since] conj (t/now)))
          db)))

    :sv.system/power-down
    (fn [db [_ system-id] conn-id]
      (let [db (update-in db [:games :sv :ship conn-id :systems system-id]
                          (fn [{:keys [in-use] :as system}]
                            (if (> in-use 0)
                              (-> system
                                  (assoc :in-use (dec in-use))
                                  (update :powered-since (comp vec butlast)))
                              system)))]
        (if (= :weapons system-id)
          (turn-off-weapons db conn-id)
          db)))

    :sv.weapon/power-up
    (fn [db [_ stuff-id] conn-id]
      (let [weapons-system (subs/system db conn-id :weapons)
            consuming-power (->> weapons-system
                                 :stuff
                                 vals
                                 (filter :charging-since)
                                 (map :required-power)
                                 (reduce +))]
        (if (>= (- (:in-use weapons-system)
                   consuming-power)
                (get-in weapons-system [:stuff stuff-id :required-power]))
          (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :charging-since] (t/now))
          db)))

    :sv.weapon/power-down
    (fn [db [_ stuff-id] conn-id]
      (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :charging-since] nil))

    :sv.weapon/hit
    (fn [db [_ target-ship-id target-system-id] conn-id]
      (reduce (fn [acc-db {:keys [id status damage]}]
                (let [shields-ready? (= :ready (:status (subs/shield-readiness acc-db target-ship-id)))]
                  (if (= :selected status)
                    (cond-> acc-db
                      :fire
                      (assoc-in [:games :sv :ship conn-id :systems :weapons :stuff id :firing-since] (t/now))
                      :drop-selection
                      (assoc-in [:games :sv :ship conn-id :systems :weapons :stuff id :is-selected] false)

                      shields-ready?
                      (assoc-in [:games :sv :ship target-ship-id :systems :shields :last-absorption] (t/now))

                      (not shields-ready?)
                      (update-in [:games :sv :ship target-ship-id :systems target-system-id]
                                 (fn [system]
                                   (-> system
                                       (update :damaged (fn [current-damage] (min (:max system) (+ current-damage damage))))
                                       (assoc :last-hit-at (t/now))
                                       (deplete-power))))

                      (and (not shields-ready?)
                           (= :weapons target-system-id))
                      (turn-off-weapons target-ship-id))
                    acc-db)))
              db
              (subs/weapons-readiness db conn-id)))

    :sv.weapon/select
    (fn [db [_ stuff-id] conn-id]
      (if (= :ready (:status (subs/weapon-readiness db conn-id stuff-id)))
        (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :is-selected] true)
        db))

    :sv.weapon/unselect
    (fn [db [_ stuff-id] conn-id]
      (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :is-selected] false))

    :sv/jump
    (fn [db [_ location-id] conn-id]
      (-> db
          (assoc-in [:games :sv :ship conn-id :location-id] (or location-id (gen-uuid)))
          (turn-off-weapons conn-id)))

    :sv.ship/loot
    (fn [db [_ ship-id] conn-id]
      (let [amount (get-in db [:games :sv :ship ship-id :cargo :scrap])]
        (-> db
            (assoc-in [:games :sv :ship ship-id :cargo :scrap] 0)
            (assoc-in [:games :sv :ship conn-id :cargo :scrap] amount))))

    :sv.store/purchase
    (fn [db _ conn-id]
      (if (>= 30 (subs/derive db [:sv.cargo/scrap] conn-id))
        (-> db
            (assoc-in [:games :sv :ship conn-id :systems :weapons :stuff :basic-laser2] (merge weapon-basic-laser
                                                                                               {:id :basic-laser2}))
            (update-in [:games :sv :ship conn-id :cargo :scrap] - 30))
        db))}))

(defn drive [db [evt-id :as evt] & params]
  (if-let [evt-fn (get @events evt-id)]
    (do (println "drive: " (pr-str evt) " " (pr-str params))
        (apply evt-fn db evt params))
    (println "!!No evt found:" (pr-str evt))))

(defn add-evt [id handler]
  (swap! events assoc id handler))
