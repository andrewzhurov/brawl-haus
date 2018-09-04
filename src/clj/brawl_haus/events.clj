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
              :games {:sv {}}})

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
   (let [db-with-race (l "ENSURE RACE:" (ensure-race db conn-id))]
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
 :chat/add-message
 (fn [db [_ conn-id text]]
   (update db :messages conj {:text text
                              :id (gen-uuid)
                              :sender conn-id
                              :received-at (now)})))




(rf/reg-event-db
 :hiccup-touch/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :hiccup-touch})))



(rf/reg-event-db
 :ccc/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :ccc-panel})))








;; Space versus

(defn create-ship [conn-id]
  {:integrity 9
   :power-hub {:max 7
               :generating 5}
   :systems {:shields {:max 4
                       :in-use 0
                       :damaged 0}
             :engines {:max 3
                       :in-use 0
                       :damaged 0}
             :weapons {:max 3
                       :damaged 0
                       :in-use 0
                       :stuff {:burst-laser-2 {:name "Burst Laser II"
                                               :required-power 2
                                               :damage 2
                                               :charge-time 200
                                               :charging-since nil
                                               :is-selected false}
                               :basic-laser {:name "Basic Laser"
                                             :required-power 1
                                             :damage 1
                                             :charge-time 100
                                             :charging-since nil
                                             :is-selected false}}}}})

(defn sv [db]
  (get-in db [:games :sv]))

(defn deplete-power [system]
  (let [rational-in-use (max (min (- (:max system) (:damaged system)) (:in-use system)) 0)]
    (assoc system :in-use rational-in-use)))

(defn turn-off-weapons [system]
  (if (:stuff system)
    (reduce (fn [acc [stuff-id stuff]]
              (assoc-in acc [:stuff stuff-id] (assoc stuff :charging-since nil)))
            system
            (:stuff system))
    system))

(def events
  {
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
   (fn [db _ conn-id]
     (-> db
         (navigate conn-id {:location-id :quit})
         (update-in [:users conn-id] dissoc :conn)))

   :home/attend
   (fn [db _ conn-id]
     (navigate db conn-id {:location-id :home-panel}))

   :sv/attend
   (fn [db _ conn-id]
     (-> db
         (assoc-in [:games :sv :ship conn-id] (create-ship conn-id))
         (navigate conn-id {:location-id :space-versus})))

   :sv.system/power-up
   (fn [db [_ system-id] conn-id]
     (let [left-power (subs/left-power db conn-id)
           {:keys [max in-use]} (subs/system db conn-id system-id)]
       (if (and (> left-power 0)
                (> (- max in-use) 0))
         (update-in db [:games :sv :ship conn-id :systems system-id :in-use] inc)
         db)))

   :sv.system/power-down
   (fn [db [_ system-id] conn-id]
     (let [db (assoc-in db [:games :sv :ship conn-id :systems system-id :in-use]
                        (max (dec (:in-use (subs/system db conn-id system-id)))
                             0))]
       (update-in db [:games :sv :ship conn-id :systems system-id] turn-off-weapons)))

   :sv.weapon/power-up
   (fn [db [_ stuff-id] conn-id]
     (let [weapons-system (subs/system db conn-id :weapons)
           consuming-power (->> weapons-system
                                :stuff
                                vals
                                (filter :charging-since)
                                (reduce (fn [acc {:keys [required-power]}] (+ acc required-power)) 0))]
       (if (>= (- (:in-use weapons-system)
                  consuming-power)
               (get-in weapons-system [:stuff stuff-id :required-power]))
         (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :charging-since] (t/now))
         db)))

   :sv.weapon/power-down
   (fn [db [_ stuff-id] conn-id]
     (assoc-in db [:games :sv :ship conn-id :systems :weapons :stuff stuff-id :charging-since] nil))

   :sv.weapon/hit
   (fn [db [_ target-ship-id target-system-id firing-stuff-id] conn-id]
     (let [{:keys [damage]} (subs/weapon db conn-id firing-stuff-id)
           {:keys [is-ready]} (subs/weapon-readiness db conn-id firing-stuff-id)]
       (if is-ready
         (update-in db [:games :sv :ship target-ship-id :systems target-system-id]
                    (fn [system]
                      (-> system
                          (update :damaged + damage)
                          (deplete-power)
                          (turn-off-weapons))))
         db)))})

(defn drive [db [evt-id :as evt] & params]
  (if-let [evt-fn (get events evt-id)]
    (do (println "drive: " (pr-str evt) " " (pr-str params))
        (apply evt-fn db evt params))
    (throw (Exception. (str "!!No evt found: " (pr-str evt))))))
