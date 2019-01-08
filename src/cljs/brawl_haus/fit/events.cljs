(ns brawl-haus.fit.events
  (:require [brawl-haus.utils :refer [l]]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.utils :as u]
            [brawl-haus.fit.player :as player]
            [brawl-haus.fit.mnemo :as mnemo]
            [brawl-haus.fit.entities :as entities]))

(defn add-evt [{:keys [current-tick] :as db} evt]
  (l (str "EVT to " (inc current-tick)) evt)
  (update-in db [:evt-history (inc current-tick)] u/conjv evt))


(defn add-ents [db ents]
  (reduce (fn [acc-db ?ent]
            #_(if (sequential? (l -1 ?ent))
                (recur acc-db ?ent))
            (add-evt acc-db [:add-ent ?ent]))
          db
          ents))

(def events
  {:to-level (fn [{:keys [evt-history current-tick level] :as db} [_ le]]
               (if (= le level)
                 db
                 (let [new-db {:level le
                               :evt-history evt-history
                               :current-tick current-tick}
                       ents (entities/the-zone le)]
                   (l 0 (reduce (fn [acc ent]
                                  ((get events :add-ent) acc [:add-ent ent]))
                                new-db
                                ents))
                   #_(-> new-db
                       (add-ents evts))
                       )))

   :mouse (fn [{:keys [mouse current-tick] :as db} [_ coords]]
            (merge db {:prev-mouse mouse
                       :angle-diff-at current-tick
                       :angle-diff (u/calc-angle mouse coords)
                       :mouse coords}))

   :add-ent (fn [db [_ ent]] (l "ADD ENT:" (assoc-in db [:entities (:id ent)] ent)))
   :set-controls (fn [db [_ id val]] (assoc-in db [:controls id] val))
   :controls (fn [db [_ action]] (update-in db [:entities :player] player/control action))
   })



(defn >evt [evt]
  (swap! state/db add-evt evt))



;; CONTROLS
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
