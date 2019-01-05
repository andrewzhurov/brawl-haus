(ns brawl-haus.sandbox2
  (:require [garden.core]
            [reagent.core :as r]
            [reagent.ratom :as ra]
            [re-frame.core :refer [reg-event-db reg-sub reg-sub-raw]]
            [brawl-haus.utils :refer [l deep-merge]]
            [clojure.data :refer [diff]]
            [brawl-haus.panels :as panels]
            [goog.string :as gstr]))

(def fire-assets
  {1 (js/Audio. "/sandbox/ak-47-1.wav")
   2 (js/Audio. "/sandbox/ak-47-2.wav")
   3 (js/Audio. "/sandbox/ak-47-3.wav")
   4 (js/Audio. "/sandbox/ak-47-4.wav")
   5 (js/Audio. "/sandbox/ak-47-5.wav")
   6 (js/Audio. "/sandbox/ak-47-6.wav")
   7 (js/Audio. "/sandbox/ak-47-7.wav")})

(defn play-fire [idx]
  (let [sound (get fire-assets idx)]
    (.pause sound)
    (set! (.-current-time sound) 0)
    (.play sound)))

(defn conjv [v? val] (if v? (conj v? val) [val]))
(def fps 30)
(def dt (/ 1000 fps))
(def rewind-speed 2)

(def styles
  [[:body {:overflow "hidden"}]
   [:svg {:cursor "crosshair"
          }]
   [:.data-reactroot {:overflow "hidden"}]

   [:.timeline {:position "absolute"
                :top 0 :right 0 :left 0
                :height "40px"
                :background "rgba(0,0,0,0.3)"
                }
    [:input {:width "100%"}]
    [:.left {:position "absolute" :top "20px" :left "10px"}]
    [:.right {:position "absolute" :top "20px" :right "10px"}]]])

(def init-controls {:up 0
                    :down 0
                    :left 0
                    :right 0
                    :crouch 0
                    :crawl 0
                    :trigger false})

(def default-time-flow 0)
(defn relative-time [ent dt]
  (cond
    (= :player (:id ent)) (* dt 2)
    (= :bullet (:type ent)) dt
    :else (/ dt 4)))

(defn time-passed [{:keys [current-tick angle-diff-at angle-diff]
                    {{{[vx vy] :v} :phys} :player} :entities :as db}]
  (let [disp-x (Math.abs vx)
        disp-y (Math.abs vy)
        curr-angle-diff (if (= angle-diff-at current-tick) (Math.abs angle-diff) 0)]
    (merge db
           {:time-passed (+ default-time-flow
                            (* 3 disp-x)
                            disp-y
                            (/ curr-angle-diff 4))})))

(def init-db {:evt-history {}
              :current-tick 0
              ;:now (Date.now)
              :prev-mouse [0 0]
              :mouse [0 0]
              :controls init-controls
              :entities {}})
(def db (r/atom init-db))

(defn update-set [coll match update-fn]
  (into #{} (map (fn [el]
                   (let [[a b ab] (diff el match)]
                     (if ab
                       (update-fn el)
                       el)))
                 coll)))
(defn look-up [coll match]
  (some #(let [[a b ab] (diff % match)]
           (when ab %))
        coll))

(declare person-to-pose)
(declare phys-v)
(declare ent-bullet)
(declare calc-angle)
(declare displacement)

(def mouse (r/cursor db [:mouse]))
(def ppos (r/cursor db [:entities :player :position]))
(def angle (ra/reaction
            (let [[pos-x pos-y] @ppos
                  angle (calc-angle [pos-x (+ pos-y 7)] @mouse)
                  facing (if (and (<= angle 90)
                                  (>= angle -90))
                           :left :right)
                  angle (if (= :right facing)
                          (- (- angle 180))
                          angle)]
              {:vec-angle (displacement [(+ pos-x 7) pos-y] @mouse)
               :angle angle
               :facing facing})))


(def events
  {:mouse (fn [{:keys [mouse current-tick] :as db} [_ coords]]
            (merge db {:prev-mouse mouse
                       :angle-diff-at current-tick
                       :angle-diff (calc-angle mouse coords)
                       :mouse coords}))

   :add-ent (fn [db [_ ent]] (assoc-in db [:entities (:id ent)] ent))
   :set-controls (fn [db [_ id val]] (assoc-in db [:controls id] val))
   :controls (fn [db [_ action]]
               (let [subj (get-in db [:entities :player])
                     desired-pose (get-in subj [:person :desired-pose])
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
                        (phys-v [0 -3])))]
                 (assoc-in db [:entities :player] new-subj)))
   })

(defn >evt [evt]
  (swap! db (fn [{:keys [current-tick] :as db}]
              (update-in db [:evt-history (inc current-tick)] conjv evt))))

(defn displacement [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])
(defn calc-angle [[x1 y1] [x2 y2]]
  (* (js/Math.atan2 (- y1 y2) (- x1 x2)) (/ 180 js/Math.PI)))

(defn comp-position [coords]
  {:position coords})

(defn comp-phys [m v a]
  {:phys {:m m
          :v v
          :a a}})
(defn phys-v [ent [v-x v-y] & [max-x dt]]
  (update-in ent [:phys :v] (fn [[now-v-x now-v-y]]
                              (if (and max-x dt)
                                [((if (neg? max-x) max min) (+ now-v-x v-x) max-x)
                                 (+ now-v-y v-y)]
                                [(+ now-v-x v-x) (+ now-v-y v-y)])
                              )))

(defn per-s [val dt]
  (* (/ val 1000) dt))

(defn phys-throttle [ent dt]
  (update-in ent [:phys :v] (fn [[v-x v-y]]
                              [(if (< (Math.abs v-x) 0.1) 0
                                   (* v-x (- 1 (per-s 1 dt))))
                               v-y
                               #_(* v-y (- 1 (per-s 3 dt)))])))

(defn comp-collision [actor?]
  {:collision {:actor? actor?
               :grounded? false}})

(defn comp-size [w h]
  {:size [w h]})

(defn comp-weapon [spec]
  {:weapon (merge spec
                  {:temp {:last-fired-at 0
                          :left-rounds 30}})})
(def ak-47
  {:weight 3.47
   :muzzle-velocity 715 ;m/s
   :rounds 30
   :rpm 60
   :cooldown-ticks 30
   :texture "/sandbox/ak-47.svg"})

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

(defn comp-chase [active?]
  {:chase {:active? active?}})

(def ent-player
  (merge {:id :player
          :type :player
          :controlled true}
         {:render {:color "orange"
                   :facing :left}}
         (comp-person {:poses {:stand {:height 40
                                       :width 10
                                       :front-speed 4
                                       :front-max 2
                                       :back-speed 3
                                       :back-max 1
                                       }
                               :crouch {:height 20
                                        :width 13
                                        :front-speed 4
                                        :front-max 1.25
                                        :back-speed 1.5
                                        :back-max 0.75}
                               :crawl {:height 10
                                       :width 40
                                       :front-speed 3
                                       :front-max 1
                                       :back-speed 3
                                       :back-max 0.5}}
                       :weight 70})
         (comp-weapon ak-47)
         (comp-position [500 100])
         (comp-phys 5 [0 0] [0 0])
         (comp-collision true)
         (comp-chase true)))


(defn move [ent move-direction dt]
  (let [{:keys [angle facing]} @angle ;; SIDECOFX
        backward? (not= move-direction facing)
        pose (person-pose ent)
        [speed max] (case [move-direction backward?]
                      [:left false] [[(- (get pose :front-speed)) 0] (- (get pose :front-max))]
                      [:left true]  [[(- (get pose :back-speed)) 0] (- (get pose :back-max))]
                      [:right false] [[(get pose :front-speed) 0] (get pose :front-max)]
                      [:right true] [[(get pose :back-speed) 0] (get pose :back-max)])]
    (l "MOVING:" speed)
    (phys-v ent [(* 6 (first speed)) (* 3 (second speed))] (* max 3) dt)))


(def ent-floor
  (merge {:id :floor
          :type :floor}
         {:render {:color "gray"}}
         (comp-position [100 300])
         (comp-size 600 52)
         (comp-collision false)))


(def ent-stairs
  (map (fn [stair-num]
         (let [w 15
               h 5]
           (merge {:id (keyword (str "stair-" stair-num))
                   :type :stair}
                  {:render {:color "darkgray"}}
                  (comp-position [(+ 600 (* stair-num w)) (- 300 (* stair-num h))])
                  (comp-size w h)
                  (comp-collision false)
                  )))
       (range 1 11)))

;; weapon in
(defn ent-bullet [{[pos-x pos-y] :position
                   }]
  (let [{[disp-x disp-y] :vec-angle
         :keys [angle]} @angle
        id (keyword (str (random-uuid)))]
    {id (merge {:id id
                :type :bullet}
               {:render {:color "steel"
                         :angle angle}}
               (comp-position [(-> (/ 40 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                   (* disp-x)
                                   (+ pos-x))
                               (-> (/ 40 (+ (Math.abs disp-x) (Math.abs disp-y)))
                                   (* disp-y)
                                   (+ pos-y 6))])
               (comp-size 4 2)
               (comp-collision true)
               (comp-phys 0.1
                          [(-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                               (* disp-x)
                               (* (/ dt 1000)))
                           (-> (/ 1000 (+ (Math.abs disp-x) (Math.abs disp-y)))
                               (* disp-y)
                               (* (/ dt 1000)))]
                          [0 0])
               {:self-destruct {:after 20000
                                :spawn-time (Date.now)}})}))



(def ent-enemy
  (merge {:id :enemy
          :type :enemy}
         (comp-position [100 100])
         (comp-size 7 15)
         (comp-collision false)
         (comp-phys 5 [0 0] [0 0])
         (comp-chase true)))

(defn collided? [{[act-x act-y] :position
                  [act-w act-h] :size}
                 {[pass-x pass-y] :position
                  [pass-w pass-h] :size}]
  (let [act-x2 (+ act-x act-w)
        act-y2 (+ act-y act-h)
        pass-x2 (+ pass-x pass-w)
        pass-y2 (+ pass-y pass-h)
        ]
    (not (or (or (> pass-x act-x2)
                 (< pass-x2 act-x))
             (or (> pass-y act-y2)
                 (< pass-y2 act-y))))))

(defn player-ground [{[player-x _] :position
                      [_ player-h] :size :as player}
                     {[_ floor-y] :position :as floor}]
  (deep-merge
   player
   {:phys {:v [(get-in player [:phys :v 0]) 0]}
    :position [player-x (- floor-y player-h 1)]
    :collision {:grounded? true}}))

(defmulti collide (fn [act pass] [(:type act) (:type pass)]))
(defmethod collide [:player :floor]
  [player floor]
  [(player-ground player floor)
   floor])

(defmethod collide [:player :stair]
  [{[w h] :size
    [player-x _] :position :as player}
   {[_ stair-y] :position :as stair}]
  [(player-ground player stair) stair])
(defmethod collide [:player :enemy]
  [p e]
  (println "HIT") [p e])

(defmethod collide :default [a b] [a b])

(def gravity-strength 9.8) ;; m/s^2
;; dt - ms
(def systems
  [;Controls
   (fn [{:keys [current-tick time-passed  controls] :as db
         {{{:keys [desired-pose]} :person :as subj} :player} :entities}]
     (let [{:keys [left right trigger]} controls
           firing? (and trigger
                        (> (get-in subj [:weapon :temp :left-rounds]) 0)
                        (< (get-in subj [:weapon :cooldown-ticks]) (- current-tick
                                                                      (get-in subj [:weapon :temp :last-fired-at]))))
           rt (relative-time subj time-passed)]
       {:entities (cond-> {:player (cond-> subj
                                     (and (> left 0.1) (get-in subj [:collision :grounded?]))
                                     (move :left rt)

                                     (and (> right 0.1) (get-in subj [:collision :grounded?]))
                                     (move :right rt)

                                     firing?
                                     ((fn [player]
                                        (let [next-sound-idx (rand-nth (into [] (clojure.set/difference #{1 2 3 4 5 6 7} (take 5 (get-in subj [:weapon :temp :played-shot-sounds])))))]
                                          (play-fire next-sound-idx)
                                          (-> player
                                              (update-in [:weapon :temp :played-shot-sounds] conj next-sound-idx)
                                              (update-in [:weapon :temp :left-rounds] dec)
                                              (assoc-in [:weapon :temp :last-fired-at] current-tick)))))

                                     (and (<= left 0.1) (<= right 0.1))
                                     (phys-throttle rt))}
                    firing? (merge (ent-bullet subj)))
        }))

   ;Displace
   (fn [{:keys [time-passed entities]}]
     {:entities (->> entities
                     (filter (comp :phys val))
                     (map (fn [[id {[x y] :position
                                    {[vx vy] :v} :phys :as subj}]]
                            (let [rt (relative-time subj time-passed)]
                              {id {:position [(+ x (* (/ (* vx 3) 1000) rt)) (+ y (* (/ (* vy 3) 1000) rt))]}}))
                          )
                     (apply merge))})

   ;Gravity
   (fn [{:keys [entities time-passed]}]
     (l "TIME PASSED:" time-passed)
     {:entities
      (->> entities
           (filter (comp :phys val))
           (map (fn [[id {{[a-x-old a-y-old] :a :keys [m]} :phys
                          {:keys [grounded?]} :collision :as subj}]]
                  (let [rt (relative-time subj time-passed)]
                    (if grounded?
                      {id subj}
                      {id (update-in subj [:phys :v] (fn [[old-x old-y]] [(+ old-x
                                                                             (* a-x-old (/ rt 1000)))
                                                                          (+ old-y
                                                                             (* a-y-old (/ rt 1000))
                                                                             (* (* (/ gravity-strength 10) m) (/ rt 1000)))]))}))))
           (apply merge))})

   ;Collision
   (fn [db & without-subjs]
     (let [without-subjs (or without-subjs #{})
           subjs (->> (:entities db)
                      (remove (fn [[id _]] (contains? without-subjs id)))
                      (filter (comp :collision val)))
           actor (first (filter (comp :actor? :collision val) subjs))
           coll-subj (some #(when (and actor (collided? (second actor) (second %))) %)
                           (disj (set subjs) actor))
           actor (second actor)
           coll-subj (second coll-subj)]
       (if coll-subj
         (recur
          (deep-merge db
                      {:entities (if coll-subj
                                   (reduce (fn [acc {:keys [id] :as ent}] (assoc acc id ent))
                                           {}
                                           (collide actor coll-subj))
                                   {})})
          (conj without-subjs (:id coll-subj) (:id actor)))
         db)))

   #_[[:chase]
    (fn [dt subjs db]
      (let [player (first (filter #(= :player (:type %)) subjs))
            enemy (first (filter #(= :enemy (:type %)) subjs))
            [disp-x _] (displacement (:position player) (:position enemy))]
        (if (neg? disp-x)
          [player (phys-v enemy [-1 0] -3 dt)]
          [player (phys-v enemy [ 1 0]  3 dt)])))]

   (fn [db]
     (let [now (Date.now)
           subjs (filter (fn [[_ {{:keys [after spawn-time]} :self-destruct}]]
                           (and spawn-time
                                (< (+ spawn-time after) now)))
                         (:entities db))
           ]
       {:entities (reduce (fn [acc [id _]] (assoc acc id nil))
                          {}
                          subjs)}
       ))])




(defn process-evts [{:keys [evt-history] :as db} at-tick]
  (reduce (fn [acc-db [evt-id :as evt]]
            ((get events evt-id) acc-db evt))
          db
          (get evt-history at-tick)))


(defn run-systems [db]
  (reduce (fn [db sys-fn]
            (deep-merge db (sys-fn db)))
          db
          systems))

(defn mnemo-tick [db-now db-prev]
  (assoc db-now :prev-tick db-prev))

(defn back-tick [db]
  (if-let [pt (reduce (fn [db _]
                        (if-let [pt-inner (:prev-tick db)]
                          pt-inner
                          db))
                      db (range rewind-speed))]
    pt
    (do
      (println "LIMIT REACHED")
      db)))

(defn drop-controls [db]
  (assoc db :controls init-controls))

(def mnemo (atom {:current-reverse nil
                  :reverse-history #{}}))

(defn normal-time? [] (not (:current-reverse @mnemo)))


(defn tick! []
  (swap! db (fn [db]
              (let [;time-skewed (* 2 (reduce + (map (fn [[from till]] (- till from)) (:reverse-history @mnemo))))
                    prev-tick (:current-tick db)
                    current-tick (inc prev-tick)
                    ;now (- (js/Date.now) time-skewed)
                    ;prev-tick (or (:current-tick db) (- (js/Date.now) 1000))
                    dt dt ;(- now prev-tick)
                    ]
                #_(if (:current-reverse @mnemo)
                    (-> db
                        (back-tick)
                        (drop-controls)))
                (-> (assoc db :current-tick current-tick)
                    (process-evts prev-tick)
                    (time-passed)
                    (run-systems)
                    #_(mnemo-tick db)))))
  )


(defonce sys-running (atom nil))
(defn stop-engine! []
  (when-let [sr @sys-running]
    (js/clearInterval sr)))
(defn start-engine! []
  (do (stop-engine!)
      (reset! sys-running (js/setInterval tick! 16))))



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
                         (swap! mnemo update :current-reverse (fn [curr] #_(l "Reverse:")  (or curr (Date.now))))

                         (when (normal-time?)
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
                         (do (swap! mnemo (fn [{:keys [current-reverse
                                                       reverse-history]}]
                                            (l "Finished reverse:"
                                               {:current-reverse nil
                                                :reverse-history (conj reverse-history [current-reverse (Date.now)])}))))
                         (when (normal-time?)
                           (let [{:keys [to on-press?]} (first (filter (fn [{:keys [which]}]
                                                                         (= which (.-which e)))
                                                                       controls))]
                             (when to
                               #_(.preventDefault e)
                               #_(.stopPropagation e)
                               (if on-press?
                                 (onpress :up [:controls to])
                                 (deduple [:set-controls to 0])))))))))


(defmulti render :type)
(defmethod render :player
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render
    :keys [weapon id]}]
  (let [{:keys [facing angle]} @angle]
    [:g {:transform (str "translate(" x "," y ")"
                         (when (= :right facing) "scale(-1, 1)"))}
     [:rect {:x 0 :y 0 :width w :height h
             :fill color}]
     [:circle {:x 4 :y 0 :r 5 :color "yellow"}]
     [:g {:x x
          :y y
          :transform (str "translate(" -40 ",0)"
                          "rotate(" angle "," 40 "," 7 ")"
                          )}

      [:image {:xlinkHref (:texture weapon)
               :width 40
               :height 20
               }]

      [:line {:stroke-width "1px"
              :stroke "url(#grad1)"
              :x1 2 :y1 7
              :x2 -500 :y 7
              }]]
     ]))

(defmethod render :bullet
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render}]
  #_[:image {:x x :y y :width w :height h
           ;:transform (gstr/format "rotate(%s, %s, %s)" angle)
           :xlinkHref "/sandbox/bullet.png"}]
  [:rect {:x x :y y :width w :height h
          :fill color}])

(defmethod render :default
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render}]
  [:rect {:x x :y y :width w :height h
          :fill color}])

(defn inspect [data x y]
  [:g
   (cond
     (map? data)
     (map-indexed (fn [idx [k v]]
                    ^{:key idx}
                    [:g
                     [:text {:x x
                             :y (+ y (* 12 (inc idx)))
                             :color :gray}
                      (pr-str k v)]])
                  data)
     :default [:text {:x x :y y :color :orange} (pr-str data)])])

(defn timeline []
  (let [{:keys [evt-history current-tick]} @db
        f-domain 0
        l-domain current-tick
        axis-domain (- l-domain f-domain)
        axis-range 1180
        ->range (fn [domain-val] (* (/ axis-range axis-domain) (- domain-val f-domain)))]
    [:g
     (for [[tick evts] evt-history
           [idx [evt-id]] (partition 2 (interleave (range) evts))
           :when (not= evt-id :mouse)]
       (do #_(js/console.log "His item:" tick idx evt-id)
         ^{:key (str tick "-" idx)}
         [:circle {:cx (->range tick) :cy (* 10 (inc idx))
                   :r 3 :fill "rgba(255,255,0,0.6)"}]))
     (when (not (normal-time?))
       [:image {:xlinkHref "/sandbox/rewind.svg"
                :x 1060 :y 40
                :width 80
                }])
     ]))

(defn coords [e] [(.-clientX e) (.-clientY e)])

(defn tutorial []
  [:g
   [:text {:x 10 :y 15}
    "WASD to move"]
   [:text {:x 10 :y 27}
    "Left mouse button to shoot"]
   [:text {:x 10 :y 40}
    "SPACE to rewind"]])

(defn content []
  (r/create-class
   {:component-will-mount
    #(do (reset! db init-db)
         (start-engine!)
         (>evt [:add-ent ent-floor])
         (doseq [stair ent-stairs]
           (>evt [:add-ent stair]))
         (>evt [:add-ent ent-player])
         (>evt [:add-ent ent-enemy]))
    :component-will-unmount
    #(stop-engine!)
    :reagent-render
    (fn [_]
      [:div
       [:style (garden.core/css styles)]
       [:svg {:style {:width "100vw"
                      :height "100vh"}
              :on-mouse-move #(when (normal-time?) (>evt [:mouse (coords %)]))
              :on-mouse-down #(>evt [:set-controls :trigger true])
              :on-mouse-up #(>evt [:set-controls :trigger false])
              }
        [:defs
         [:linearGradient#grad1
          {:y2 "0%", :x2 "100%", :y1 "0%", :x1 "0%"}
          [:stop
           {:style {:stop-color "rgb(255,100,0)"
                    :stop-opacity "0"},
            :offset "0%"}]
          [:stop
           {:style {:stop-color "rgb(255,100,0)"
                    :stop-opacity "0.4"},
            :offset "100%"}]]]
        [timeline]
        #_[tutorial]
        (for [[id ent] (sort-by key (:entities @db))]
          ^{:key id}
          [render ent])
        [inspect (get-in @db [:entities :player :phys]) 10 50]
        #_[inspect (:controls @db) 10 30]
        [:text {:x 100 :y 100} @angle]
        ]])}))

(defmethod panels/panel :frozen-in-time
  [_]
  [content]
  )
