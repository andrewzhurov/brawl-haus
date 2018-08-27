(ns brawl-haus.shared.events
  (:require #?@(:clj [[clj-time.core :as t]
                      [clj-time.coerce :as c]]
                :cljs [[cljs-time.core :as t]
                       [cljs-time.coerce :as c]])
            [brawl-haus.shared.data :as data]
            [re-frame.core :as rf]
            ))

(defn l [desc expr] (println desc expr) expr)
(defn location [state conn-id]
  (get-in state [:users conn-id :location]))
(defn gen-uuid [] #?(:clj (str (java.util.UUID/randomUUID))
                     :cljs (str (rand-int 9999999))))
(defn now [] (java.util.Date.))


; hardcoded to start in 10 sec
(defn schedule-race [race]
  (assoc race :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date))))

#_(defn ready-set-go [{:keys [id] :as race}]
  (future (Thread/sleep 7000)
          (swap! state/public-state update-in [:open-races id]
                 merge {:status :began
                        :race-text (rand-nth data/long-texts)}))
  race)

(defn calc-speed [text start finish]
  (let [minutes (/ (t/in-seconds (t/interval (c/from-date start)
                                             (c/from-date finish)))
                   60)
        chars (count text)]
    (int (/ chars minutes))))

(defn race-to-be [state]
  (let [[[_ race]] (filter (fn [[_ {:keys [status]}]] (= :to-be status))
                           (get state :open-races))]
    race))

(defn ensure-race [state]
  (if-not (race-to-be state)
    (let [new-race (-> {:id (gen-uuid)
                        :participants {}
                        :status :to-be}
                       schedule-race
                       #_ready-set-go)]
      (assoc-in state [:open-races (:id new-race)] new-race))
    state))

(defn navigate [state conn-id location]
  (assoc-in state [:users conn-id :location] location))

(defn enter-race [state conn-id]
  (let [race (race-to-be state)]
    (-> state
        (assoc-in [:open-races (:id race) :participants conn-id] {})
        (navigate conn-id {:location-id :race-panel
                           :params {:race-id (:id race)}}))))

(declare drive)

(rf/reg-event-db
 :chat/add-message
 (fn [db [_ {:keys [conn-id]} text]]
   (update db :messages conj {:text text
                              :id (gen-uuid)
                              :sender conn-id
                              :received-at (now)})))

(rf/reg-event-db
 :chat/set-nick
 (fn [db [_ {:keys [conn-id]} nick]]
   (assoc-in db [:users conn-id :nick] nick)))

(def event-handlers
  {
   :hiccup-touch/attend
   (fn [state conn-id _]
     (navigate state conn-id {:location-id :hiccup-touch}))

   :home/attend
   (fn [state conn-id _]
     (navigate state conn-id {:location-id :home-panel}))

   :ccc/attend
   (fn [state conn-id _]
     (navigate state conn-id {:location-id :ccc-panel}))

   :race/attend
   (fn [state conn-id _]
     (-> state
         ensure-race
         (enter-race conn-id)))

   :race/left-text
   (fn [state conn-id [_ left-text]]
     (let [is-finished (zero? (count left-text))
           race-id (-> (location state conn-id) :params :race-id)
           {:keys [race-text starts-at]} (get-in state [:open-races race-id])]
       (assoc-in state
                 [:open-races race-id :participants conn-id]
                 {:left-chars (count left-text)
                  :speed (calc-speed race-text starts-at (now))})))
   })


(rf/reg-event-db
 :conn/on-close
 (fn [db [_ {:keys [conn-id]}]]
   (-> db
       (navigate conn-id {:location-id :quit})
       (update-in [:users conn-id] dissoc :conn))))

(rf/reg-event-db
 :conn/on-create
 (fn [db [_ {:keys [conn conn-id]}]]
   (let [anonymous-user {:nick (rand-nth data/names)
                         :conn-id conn-id
                         :conn conn}]
     (-> db
         (assoc-in [:users conn-id] anonymous-user)
         (navigate conn-id {:location-id :home-panel})))))
