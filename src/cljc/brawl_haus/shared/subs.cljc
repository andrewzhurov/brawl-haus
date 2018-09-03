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

(rf/reg-sub
 :set-nick
 (fn [db [_ conn-id]]
   {:current-nick (get-in db [:users conn-id :nick])}))

(rf/reg-sub
 :location
 (fn [db [_ conn-id]]
   (location db conn-id)))

(rf/reg-sub
 :user
 (fn [db [_ _ conn-id]]
   (get-in db [:users conn-id])))

(rf/reg-sub
 :self
 (fn [db [_ conn-id]]
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
 (fn [db [_ conn-id]]
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


;; Space versus
(rf/reg-sub
 :sv
 (fn [db _]
   (get-in db [:games :sv])))

(rf/reg-sub
 :sv/power-hub
 (fn [db [_ conn-id]]
   (l "POWER HUB:" (get-in db [:games :sv :ship conn-id :power-hub]))))

(rf/reg-sub
 :sv/systems
 (fn [db [_ conn-id]]
   (get-in db [:games :sv :ship conn-id :systems])))

(rf/reg-sub
 :view.sv/systems
 (fn [[_ conn-id]]
   (rf/subscribe [:sv/systems conn-id]))
 (fn [systems _]
   (map (fn [[system-id {:keys [max in-use]}]]
          {:system-id system-id
           :max max
           :in-use in-use})
        (l "SYSS:" systems))))

(rf/reg-sub
 :sv.weapons/stuff
 (fn [[_ conn-id]]
   [(rf/subscribe [:sv/systems conn-id])])
 (fn [[systems] [_ _ {:keys [stuff-id]}]]
   (get-in systems [:weapons :stuff])))

(rf/reg-sub
 :sv.weapons.stuff/view
 (fn [[_ conn-id]]
   (rf/subscribe [:sv.weapons/stuff conn-id]))
 (fn [stuff _]
   (map (fn [[stuff-id a-stuff]]
          (assoc (select-keys a-stuff [:name :required-power])
                 :stuff-id stuff-id))
        stuff)))

(rf/reg-sub
 :sv/weapon
 (fn [[_ conn-id]]
   [(rf/subscribe [:sv/systems conn-id])])
 (fn [[systems] [_ _ {:keys [stuff-id]}]]
   (get-in systems [:weapons :stuff stuff-id])))

(rf/reg-sub
 :sv/left-power
 (fn [[_ conn-id]]
   [(rf/subscribe [:sv/power-hub conn-id])
    (rf/subscribe [:sv/systems conn-id])])
 (fn [[{:keys [generating]} systems] [_ conn-id]]
   (let [power-in-use (reduce (fn [acc [_ system]] (+ acc (:in-use system))) 0 systems)]
     (l 11 generating)
     (l 22 power-in-use)
     (- generating power-in-use))))

(rf/reg-sub
 :sv/used-power
 (fn [[_ conn-id]]
   [(rf/subscribe [:sv/power-hub conn-id])
    (rf/subscribe [:sv/left-power conn-id])])
 (fn [[{:keys [max]} left-power] [_ conn-id]]
   (- max left-power)))

(rf/reg-sub
 :sv.power/info
 (fn [[_ conn-id]]
   [(rf/subscribe [:sv/used-power conn-id])
    (rf/subscribe [:sv/left-power conn-id])
    (rf/subscribe [:sv/power-hub conn-id])])
 (fn [[used left power-hub]_]
   {:used used
    :left left
    :max (:max power-hub)}))

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

(rf/reg-sub
 :sv.weapon/readiness
 (fn [[_ conn-id {:keys [stuff-id]}]]
   (rf/subscribe [:sv/weapon conn-id {:stuff-id stuff-id}]))
 (fn [{:keys [charge-time charging-since]} _]
   (calc-readiness charge-time charging-since)))


