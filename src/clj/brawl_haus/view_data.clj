(ns brawl-haus.view-data)

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

(defn countdown [state conn-id]
  (select-keys (current-race state conn-id) [:starts-at]))

(defn text-race [state conn-id]
  (let [race (current-race state conn-id)]
    {:has-began (empty? (:race-text race))
     :race-text (:race-text race)
     :starts-at (:starts-at race)}))

(defn race-progress [state conn-id]
  (->> (location state conn-id)
       :params
       :race-id
       (race-progress* state)))


(defn location [state conn-id]
  (get-in state [:users conn-id :location]))

(defn participants [state _]
  (->> (:users state)
       vals
       (remove #(= :quit (get-in % [:location :location-id])))
       (map :nick)
       (sort)))

(defn set-nick [state conn-id]
  {:current-nick (get-in state [:users conn-id :nick])})

(def view-data-fns
  {:countdown countdown
   :text-race text-race
   :race-progress race-progress
   :location location

   :participants participants
   :set-nick set-nick})
