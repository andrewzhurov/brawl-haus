(ns brawl-haus.events
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.data :as data]))

(defn l [desc expr] (println desc expr) expr)

; Hardcoded to start in 10 sec
#?(:clj
   (do
     (defn uuid [] (str (java.util.UUID/randomUUID)))
     (defn now [] (java.util.Date.))
     (defn schedule-race [race]
       (assoc race :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date))))
     
     #_(defn ready-set-go [{:keys [id] :as race}]
         (future (Thread/sleep 7000)
                 (swap! public-state update-in [:open-races id]
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
         (let [new-race (-> {:id (uuid)
                             :participants {}
                             :status :to-be}
                            schedule-race
                            #_ready-set-go)]
           (assoc-in state [:open-races (:id new-race)] new-race))
         state))

     (defn navigate [state tube-id location]
       (assoc-in state [:users tube-id :location] location))

     (defn enter-race [state conn-id]
       (let [race (race-to-be state)]
         (-> state
             (assoc-in [:open-races (:id race) :participants conn-id] {})
             (navigate conn-id {:location-id :race-panel
                                :params {:race-id (:id race)}}))))))

(declare drive)

(defn location [state conn-id]
  (get-in state [:users conn-id :location]))

(def event-handlers
  {:conn/on-create
   (fn [state [_ conn-id]]
     (let [anonymous-user {:nick (rand-nth data/names)}]
       (-> state
           (assoc-in [:users conn-id] anonymous-user)
           (drive [:race/attend] conn-id))))

   ;:tube/on-destroy
   ;(fn [tube _]
   ;  (swap! public-state
   ;         navigate (id tube) {:location-id :quit}))

   ;:add-message
   ;(fn [tube [_ text]]
   ;  (when-let [user (l "USER?: "(tube->user (id tube)))]
   ;    (swap! public-state update :messages conj {:text text
   ;                                               :id (uuid)
   ;                                               :sender (id tube)
   ;                                               :received-at (now)}))
   ;  tube)

   :race/attend
   (fn [state _ conn-id]
     #?(:clj (-> state
                 ensure-race
                 (enter-race conn-id))
        :cljs state))

   ;:race/left-text
   ;(fn [state [_ race-id left-text] conn-id]
   ;  (let [is-finished (zero? (count left-text))
   ;        {:keys [race-text starts-at]} (get-in state [:open-races race-id])]
   ;    (assoc-in state
   ;              [:open-races race-id :participants conn-id]
   ;              {:left-chars (count left-text)
   ;               :speed (calc-speed race-text starts-at (now))})))

   ;:chat/set-nick
   ;(fn [state [_ nick] conn-id]
   ;  (assoc-in state [:users conn-id :nick] nick))
   })

(defn user [state conn-id]
  (get-in state [:users conn-id]))

(defn race-progress [state race-id]
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
         :did-finish (not (zero? left-chars))
         :did-quit (= (:location-id location) :quit)}))))

(defn drive
  [state & [[evt-id] :as params]]
  (let [event-handler (get event-handlers evt-id)]
    (when (nil? event-handler) (throw (Exception. (str "Unable to find event handler for: " (pr-str params)))))
    (apply event-handler state params)))
