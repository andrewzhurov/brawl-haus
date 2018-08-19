(ns brawl-haus.events
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [brawl-haus.conn :as conn]
            [brawl-haus.state :as state]
            [brawl-haus.data :as data]
            [brawl-haus.view-data :as view-data]))

(defn l [desc expr] (println desc expr) expr)
(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))

; hardcoded to start in 10 sec
(defn schedule-race [race]
  (assoc race :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date))))

(defn ready-set-go [{:keys [id] :as race}]
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
    (let [new-race (-> {:id (uuid)
                        :participants {}
                        :status :to-be}
                       schedule-race
                       ready-set-go)]
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

(def event-handlers
  {:conn/on-create
   (fn [state conn-id _]
     (let [anonymous-user {:nick (rand-nth data/names)}]
       (-> state
           (assoc-in [:users conn-id] anonymous-user)
           (drive conn-id [:race/attend]))))
   
   ;:tube/on-destroy
   ;(fn [tube _]
   ;  (swap! public-state
   ;         navigate (id tube) {:location-id :quit}))
   
   :chat/add-message
   (fn [state conn-id [_ text]]
     (update state :messages conj {:text text
                                   :id (uuid)
                                   :sender conn-id
                                   :received-at (now)}))
   
   :hiccup-touch/attend
   (fn [state conn-id _]
     (navigate state conn-id {:location-id :hiccup-touch}))

   :race/attend
   (fn [state conn-id _]
     (-> state
         ensure-race
         (enter-race conn-id)))
   
   :race/left-text
   (fn [state conn-id [_ left-text]]
     (let [is-finished (zero? (count left-text))
           race-id (-> (view-data/location state conn-id) :params :race-id)
           {:keys [race-text starts-at]} (get-in state [:open-races race-id])]
       (assoc-in state
                 [:open-races race-id :participants conn-id]
                 {:left-chars (count left-text)
                  :speed (calc-speed race-text starts-at (now))})))
   
   :chat/set-nick
   (fn [state conn-id [_ nick]]
     (assoc-in state [:users conn-id :nick] nick))


   :view-data/subscribe
   (fn [state conn-id [_ view-id]]
     (if (contains? view-data/view-data-fns view-id)
       (assoc-in state [:users conn-id :view-data-subs view-id] {})
       (do (throw (Exception. (str "No view-data-fn registered for: " view-id)))
           state)))

   :view-data/unsubscribe
   (fn [state conn-id [_ view-id]]
     (update-in state [:users conn-id :view-data-subs] dissoc conn-id))})
   

(defn drive
  [state conn-id [evt-id :as evt]]
  (let [event-handler (get event-handlers evt-id)]
    (when (nil? event-handler) (throw (Exception. (str "Unable to find event handler for: " evt))))
    (event-handler state conn-id evt)))
