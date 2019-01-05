(ns brawl-haus.fit.player
  (:require [brawl-haus.utils :refer [l deep-merge]]
            [brawl-haus.fit.sound :as sound]
            [brawl-haus.fit.time :as time]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.phys :as phys]
            [brawl-haus.fit.weapon :as weapon]
            [brawl-haus.fit.collision :as collision]
            [brawl-haus.fit.chase :as chase]
            [brawl-haus.fit.misc :refer [comp-position]]
            [brawl-haus.fit.events :refer [>evt]]
            [brawl-haus.fit.mnemo :as mnemo]
            [brawl-haus.fit.entities :as entities]))

(defn comp-person [spec]
  {:person (merge spec
                  {:desired-pose :stand})
   :size [(get-in spec [:poses :stand :width]) (get-in spec [:poses :stand :height])]})

(defn person-pose [ent & [pose]]
  (get-in ent [:person :poses (or pose (get-in ent [:person :desired-pose]))]))
(defn person-to-pose [person pose]
  (let [{:keys [width height]} (person-pose person pose)]
    (-> person
        (assoc-in [:person :desired-pose] pose)
        (assoc-in [:collision :grounded?] false)
        (merge {:size [width height]}))))

(defn move [ent move-direction dt]
  (let [{:keys [angle facing]} @state/angle ;; SIDECOFX
        backward? (not= move-direction facing)
        pose (person-pose ent)
        [speed max] (case [move-direction backward?]
                      [:left false] [[(- (get pose :front-speed)) 0] (- (get pose :front-max))]
                      [:left true]  [[(- (get pose :back-speed)) 0] (- (get pose :back-max))]
                      [:right false] [[(get pose :front-speed) 0] (get pose :front-max)]
                      [:right true] [[(get pose :back-speed) 0] (get pose :back-max)])]
    (phys/push-v ent [(* 6 (first speed)) (* 3 (second speed))] (* max 3) dt)))

(defn player-ground [{[player-x _] :position
                      [_ player-h] :size :as player}
                     {[_ floor-y] :position :as floor}]
  (deep-merge
   player
   {:phys {:v [(get-in player [:phys :v 0]) 0]}
    :position [player-x (- floor-y player-h 1)]
    :collision {:grounded? true}}))

(defn control [subj action]
  (let [desired-pose (get-in subj [:person :desired-pose])
        new-subj
        (cond-> subj
          :change-pose
          (person-to-pose (case [action desired-pose]
                            [:up :crawl]  :crouch
                            [:up :crouch] :stand
                            [:up :stand]  :stand
                            [:down :stand]  :crouch
                            [:down :crouch] :crawl
                            [:down :crawl]  :crawl
                            [:crouch :stand] :crouch
                            [:crouch :crouch] :stand
                            [:crouch :crawl] :crouch
                            [:crawl :stand] :crawl
                            [:crawl :crouch] :crawl
                            [:crawl :crawl] :stand))
          (and (= action :up)
               (= desired-pose :stand)
               (get-in subj [:collision :grounded?]))
          (->
           (deep-merge {:collision {:grounded? false}})
           (phys/push-v [0 -3])))]
    new-subj
    ))

(def system
  (fn [{:keys [current-tick time-passed  controls] :as db
        {{{:keys [desired-pose]} :person :as subj} :player} :entities}]
    (let [{:keys [left right trigger]} controls
          firing? (and trigger
                       (> (get-in subj [:weapon :temp :left-rounds]) 0)
                       (< (get-in subj [:weapon :cooldown-ticks]) (- current-tick
                                                                     (get-in subj [:weapon :temp :last-fired-at]))))
          rt (time/relative-time subj time-passed)]
      {:entities (cond-> {:player (cond-> subj
                                    (and (> left 0.1) (get-in subj [:collision :grounded?]))
                                    (move :left rt)

                                    (and (> right 0.1) (get-in subj [:collision :grounded?]))
                                    (move :right rt)

                                    firing?
                                    ((fn [player]
                                       (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6 7} (take 5 (get-in subj [:weapon :temp :played-shot-sounds])))))]
                                         (sound/play-fire next-sound-idx)
                                         (-> player
                                             (update-in [:weapon :temp :played-shot-sounds] conj next-sound-idx)
                                             (update-in [:weapon :temp :left-rounds] dec)
                                             (assoc-in [:weapon :temp :last-fired-at] current-tick)))))

                                    (and (<= left 0.1) (<= right 0.1))
                                    (phys/throttle rt))}
                   firing? (merge (entities/bullet subj)))
       })))


;; Controls
(def controls
  [{:which 65
    :to :left
    :on-press? false}
   {:which 69
    :to :right
    :on-press? false}

   {:which 188
    :to :up
    :on-press? true}
   {:which 79
    :to :down
    :on-press? true}
   #_{:which 17
      :to :crouch
      :on-press? true}
   #_{:which 74
      :to :crawl
      :on-press? true}])

(def dedupler (atom {}))
(defn deduple [[evt-id & params :as evt]]
  (swap! dedupler
         (fn [x]
           (if (= (get @dedupler evt-id) params)
             x
             (do (>evt evt)
                 (assoc x evt-id params))))))

(def debouncer (atom {:last-evt nil
                      :last-at nil}))
(defn debounce [evt]
  (let [now (Date.now)]
    (swap! debouncer (fn [{:keys [last-evt last-at]}] (when-not (and (< (- now last-at) 50)
                                                                     (= last-evt evt))
                                                        (>evt evt))
                       {:last-evt evt
                        :last-at now}
                       ))))

(def onpresser (atom {:example-event-id :down}))
(defn onpress [direction [evt-id :as evt]]
  (swap! onpresser update evt-id (fn [prev-direction]
                                   (case [prev-direction direction]
                                     [nil :down] (do (>evt evt)
                                                     :down)
                                     [:down :down] :down
                                     [:down :up]   :up
                                     [:up :up]     :up
                                     [:up :down] (do (>evt evt)
                                                     :down)
                                     ))))

(defonce bla
  (.addEventListener js/window "keydown"
                     (fn [e]
                       (if (= 32 (.-which e))
                         (swap! mnemo/mnemo update :current-reverse (fn [curr] (or curr (Date.now))))

                         (when (mnemo/normal-time?)
                           (let [{:keys [to on-press?]} (first (filter (fn [{:keys [which]}]
                                                                         (= which (.-which e)))
                                                                       controls))]
                             (when to
                               #_(.preventDefault e)
                               #_(.stopPropagation e)
                               (if on-press?
                                 (onpress :down [:controls to])
                                 (deduple [:set-controls to 1])))))))))

(defonce bla
  (.addEventListener js/window "keyup"
                     (fn [e]
                       (if (= 32 (.-which e))
                         (do (swap! mnemo/mnemo (fn [{:keys [current-reverse
                                                             reverse-history]}]
                                                  (l "Finished reverse:"
                                                     {:current-reverse nil
                                                      :reverse-history (conj reverse-history [current-reverse (Date.now)])}))))
                         (when (mnemo/normal-time?)
                           (let [{:keys [to on-press?]} (first (filter (fn [{:keys [which]}]
                                                                         (= which (.-which e)))
                                                                       controls))]
                             (when to
                               #_(.preventDefault e)
                               #_(.stopPropagation e)
                               (if on-press?
                                 (onpress :up [:controls to])
                                 (deduple [:set-controls to 0])))))))))
