(ns brawl-haus.shared.subs
  (:require [re-frame.core :as rf]
            #?@(:clj [[clj-time.core :as t]
                      [clj-time.format :as f]
                      [clj-time.coerce :as c]]
                :cljs [[cljs-time.core :as t]
                       [cljs-time.format :as f]
                       [cljs-time.coerce :as c]])))

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



(rf/reg-sub
 :set-nick
 (fn [db [_ {:keys [conn-id]}]]
   {:current-nick (get-in db [:users conn-id :nick])}))




(rf/reg-sub
 :entry-point
 (fn [db [_ {:keys [conn-id]}]]
   {:location (location db conn-id)}))

(rf/reg-sub
 :user
 (fn [db [_ _ conn-id]]
   (get-in db [:users conn-id])))

(rf/reg-sub
 :self
 (fn [db [_ {:keys [conn-id]}]]
   (select-keys (get-in db [:users conn-id]) [:conn-id :nick])))

(rf/reg-sub
 :participants
 (fn [db _]
   (->> (:users db)
        vals
        (remove #(= :quit (get-in % [:location :location-id])))
        (map :nick)
        (sort))))

(rf/reg-sub
 :messages
 (fn [db [_ {:keys [conn-id]}]]
   {:is-empty (= 0 (count (:messages db)))
    :messages (->> (:messages db)
                   (sort-by :received-at)
                   (reverse)
                   (map (fn [{:keys [id sender text received-at]}]
                          {:id id
                           :nick (:nick (user db sender))
                           :is-my (= (:nick (user db sender)) (:nick (user db conn-id)))
                           :text text
                           :received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))})))}))
