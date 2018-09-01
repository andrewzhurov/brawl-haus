(ns brawl-haus.shared.events
  (:require #?@(:clj [[clj-time.core :as t]
                      [clj-time.coerce :as c]]
                :cljs [[cljs-time.core :as t]
                       [cljs-time.coerce :as c]])
            [re-frame.events]
            [re-frame.cofx]
            [re-frame.fx]
            [re-frame.std-interceptors]
            [brawl-haus.shared.data :as data]
            [re-frame.core :as rf]
            ))

(defn l [desc expr] (println desc expr) expr)
(def <sub (comp deref rf/subscribe))
(rf/reg-event-db
 :inspect-db
 (fn [db _]
   (println "DB:" db)
   db))

(defn location [state conn-id]
  (get-in state [:users conn-id :location]))
(defn navigate [state conn-id location]
  (assoc-in state [:users conn-id :location] location))

(defn gen-uuid [] #?(:clj (str (java.util.UUID/randomUUID))
                     :cljs (str (rand-int 9999999))))
(defn now [] (java.util.Date.))

(rf/reg-event-db
 :race/ready-set-go
 (fn [db [_ _ race-id]]
   (l "RAN"
      (update-in db [:open-races race-id]
                 merge {:status :began
                        :race-text (rand-nth data/long-texts)}))))

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
   #?(:clj (do (Thread/sleep ms)
               (rf/dispatch evt))
      :cljs nil)))

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
 :chat/set-nick
 (fn [db [_ conn-id nick]]
   (assoc-in db [:users conn-id :nick] nick)))


(rf/reg-event-db
 :hiccup-touch/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :hiccup-touch})))

(rf/reg-event-db
 :home/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :home-panel})))

(rf/reg-event-db
 :ccc/attend
 (fn [db [_ conn-id]]
   (navigate db conn-id {:location-id :ccc-panel})))



(rf/reg-event-db
 :conn/on-close
 (fn [db [_ conn-id]]
   (-> db
       (navigate conn-id {:location-id :quit})
       (update-in [:users conn-id] dissoc :conn))))

(rf/reg-event-db
 :conn/on-create
 (fn [db [_ _ {:keys [conn-id conn]}]]
   (let [anonymous-user {:nick (rand-nth data/names)
                         :conn-id conn-id
                         :conn conn}]
     (-> db
         (assoc-in [:users conn-id] anonymous-user)
         (navigate conn-id {:location-id :home-panel})))))


;; Space versus

(defn create-ship [conn-id]
  {:power-hub {:max 5
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
                       :stuff {:burst-laser-2 {:required-power 2
                                               :damage 2
                                               :charge-time 200
                                               :charging-since nil}
                               :basic-laser {:required-power 1
                                             :damage 1
                                             :charge-time 500
                                             :charging-since nil}}}}})

(defn sv [db]
  (get-in db [:games :sv]))

(rf/reg-event-db
 :sv/attend
 (fn [db [_ conn-id]]
   (-> db
       (assoc-in [:games :sv :ship conn-id] (create-ship conn-id))
       (navigate conn-id {:location-id :space-versus}))))

(defn save-deplete-power [player]
  (reduce (fn [acc [system-id {:keys [max powered damaged]}]]
            (let [power-overuse (- (+ powered damaged) max)]
              (if (pos? power-overuse)
                (update-in [:systems system-id :powered dec power-overuse])
                acc)))
          player (:systems player)))

(rf/reg-event-db
 :sv.system/power-up
 (fn [db [_ conn-id {:keys [system-id]}]]
   (let [left-power (l "LEFT POWER:" (<sub [:sv/left-power conn-id]))
         {:keys [max in-use]} (l "SYSTEMS:" (get (<sub [:sv/systems conn-id]) system-id))]
     (if (and (> left-power 0)
              (> (l "LEFT TILL MAX:" (- max in-use)) 0))
       (update-in db [:games :sv :ship conn-id :systems system-id :in-use] inc)
       db))))

(rf/reg-event-db
 :sv.weapon/power-up
 (fn [db [_ conn-id {:keys [stuff-id]}]]
   (update-in db [:games :sv :ship conn-id]
              (fn [ship]
                (let [weapons-system (get-in ship [:systems :weapons])
                      consuming-power (->> weapons-system
                                           :stuff
                                           vals
                                           (filter :charging-since)
                                           (reduce (fn [acc {:keys [required-power]}] (+ acc required-power)) 0))]
                  (if (>= (- (:in-use (l "WS:"weapons-system))
                             (l "IN USE" consuming-power))
                          (l "REQ: "(get-in weapons-system [:stuff stuff-id :required-power])))
                    (assoc-in ship [:systems :weapons :stuff stuff-id :charging-since] (t/now))
                    ship))))))

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

(rf/reg-event-db
 :sv.weapon/hit
 (fn [db [_ conn-id {:keys [ship-id system-id stuff-id]}]]
   (let [{:keys [damage]} (l "W:" (<sub [:sv/weapon conn-id {:stuff-id stuff-id}]))
         {:keys [is-ready]} (l "READINESS:" (<sub [:sv.weapon/readiness conn-id {:stuff-id stuff-id}]))]
     (if is-ready
       (update-in db [:games :sv :ship ship-id :systems system-id]
                  (fn [system]
                    (-> system
                        (update :damaged + damage)
                        (deplete-power)
                        (turn-off-weapons))))
       db))))
