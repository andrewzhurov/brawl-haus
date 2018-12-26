(ns brawl-haus.sandbox2
  (:require [garden.core]
            [reagent.core :as r]
            [reagent.ratom :as ra]
            [re-frame.core :refer [reg-event-db reg-sub reg-sub-raw]]
            [brawl-haus.utils :refer [l deep-merge ]]
            [clojure.data :refer [diff]]
            [brawl-haus.panels :as panels]
            ))

(defn conjv [v? val] (if v? (conj v? val) [val]))
(def fps 60)
(def dt (/ 1000 fps))
(def rewind-speed 2)

(def styles
  [[:body {:overflow "hidden"}]
   [:svg {:cursor "crosshair"}]
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
                    :crawl 0})

(def db (r/atom {:evt-history {}
                 :current-tick 0
                 ;:now (Date.now)
                 :mouse [0 0]
                 :controls init-controls
                 :entities #{}}))

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
(def events
  {:mouse (fn [db [_ coords]]
            (-> (assoc db :mouse coords)
                (update :entities update-set {:id "player"} (fn [{[pos-x pos-y] :position :as player}]
                                                             (let [angle (calc-angle [pos-x (+ pos-y 7)] coords)
                                                                    facing (if (and (<= angle 90)
                                                                                    (>= angle -90))
                                                                             :left :right)
                                                                    angle (if (= :right facing)
                                                                            (- (- angle 180))
                                                                            angle)]
                                                                (merge player {:vec-angle (displacement [(+ pos-x 7) pos-y] coords)
                                                                               :angle angle
                                                                               :facing facing}))))))
   :fire (fn [db _]
           (let [player (look-up (:entities db) {:id "player"})]
             (update db :entities conj (ent-bullet player))))

   :add-ent (fn [db [_ ent]] (update db :entities conj ent))
   :set-controls (fn [db [_ id val]] (assoc-in db [:controls id] val))
   :controls (fn [db [_ action]]
               (let [subj (first (filter :person (:entities db)))
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
                 (update db :entities (fn [ents]
                                        (-> ents
                                            (disj subj)
                                            (conj new-subj))))))
   })

(defn >evt [evt]
  (swap! db (fn [{:keys [current-tick] :as db}]
              (update-in db [:evt-history current-tick] conjv evt))))

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
                                [((if (neg? max-x) max min) (+ now-v-x (* v-x (/ dt 1000))) max-x)
                                 (+ now-v-y (* v-y (/ dt 1000)))]
                                [(+ now-v-x v-x) (+ now-v-y v-y)])
                              )))

(defn per-s [val dt]
  (* val (/ dt 1000)))

(defn phys-throttle [ent dt]
  (update-in ent [:phys :v] (fn [[v-x v-y]]
                              [(* v-x (- 1 (per-s 3 dt)))
                               (* v-y (- 1 (per-s 3 dt)))])))

(defn comp-collision [actor?]
  {:collision {:actor? actor?
               :grounded? false}})

(defn comp-size [w h]
  {:size [w h]})

(defn comp-weapon [spec]
  {:weapon (merge spec
                  {:temp {:last-fired nil
                          :left-rounds 30}})})
(def ak-47
  {:weight 3.47
   :muzzle-velocity 715 ;m/s
   :rounds 30
   :rpm 600
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
  (merge {:id "player"
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


(defn move [{:keys [angle facing] :as ent} move-direction dt]
  (let [backward? (not= move-direction facing)
        pose (person-pose ent)
        [speed max] (case [move-direction backward?]
                      [:left false] [[(- (get pose :front-speed)) 0] (- (get pose :front-max))]
                      [:left true]  [[(- (get pose :back-speed)) 0] (- (get pose :back-max))]
                      [:right false] [[(get pose :front-speed) 0] (get pose :front-max)]
                      [:right true] [[(get pose :back-speed) 0] (get pose :back-max)])]
    (phys-v ent speed max dt)))


(def ent-floor
  (merge {:id "floor"
          :type :floor}
         {:render {:color "gray"}}
         (comp-position [100 300])
         (comp-size 600 52)
         (comp-collision false)))


(def ent-stairs
  (map (fn [stair-num]
         (let [w 15
               h 5]
           (merge {:id (str "stair-" stair-num)
                   :type :stair}
                  {:render {:color "darkgray"}}
                  (comp-position [(+ 600 (* stair-num w)) (- 300 (* stair-num h))])
                  (comp-size w h)
                  (comp-collision false)
                  )))
       (range 1 11)))

;; weapon in
(defn ent-bullet [{[pos-x pos-y] :position
                   [disp-x disp-y] :vec-angle}]
  (merge {:id (str (random-uuid))
          :type :bullet}
         {:render {:color "steel"}}
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
         {:self-destruct {:after 600
                          :spawn-time (Date.now)}}))



(def ent-enemy
  (merge {:id "enemy"
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
  (and (or (and (>= act-x pass-x)
                (<= act-x (+ pass-x pass-w)))
           (and (<= (+ act-x act-w) pass-x
                    (+ act-x act-w) (+ pass-x pass-w))))
       (or (and (>= act-y pass-y)
                (<= act-y (+ pass-y pass-h)))
           (and (>= (+ act-y act-h) pass-y)
                (<= (+ act-y act-h) (+ pass-y pass-h))))))

(defn player-ground [{[player-x _] :position
                      [_ player-h] :size :as player}
                     {[_ floor-y] :position :as floor}]
  (deep-merge
   player
   {:phys {:v [(get-in player [:phys :v 0]) 0]}
    :position [player-x (- floor-y player-h)]
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
  [
   [;controls
    [:controlled]
    (fn [dt [{{:keys [desired-pose]} :person :as subj}]]
      (let [{:keys [left right]} (:controls @db)]
        [(if (get-in subj [:collision :grounded?])
           (cond-> subj
             (> left 0.1)
             (move :left dt)

             (> right 0.1)
             (move :right dt)

             (and (<= left 0.1) (<= right 0.1))
             (phys-throttle dt))
           subj)]))]

   [;:displace
    [:position :phys]
    (fn [dt subjs]
      (map (fn [{[x y] :position
                 {[vx vy] :v} :phys :as subj}]
             (merge subj
                    {:position [(+ x vx) (+ y vy)]}))
           subjs))]

   [;:gravity
    [:position :phys]
    (fn [dt subjs]
      (map (fn [{{[a-x-old a-y-old] :a :keys [m]} :phys
                 {:keys [grounded?]} :collision :as subj}]
             (if grounded?
               subj
               (update-in subj [:phys :v] (fn [[old-x old-y]] [(+ old-x
                                                                  (* a-x-old (/ dt 1000)))
                                                               (+ old-y
                                                                  (* a-y-old (/ dt 1000))
                                                                  (* (* (/ gravity-strength 10) m) (/ dt 1000)))]))))
           subjs))]

   [;collision
    [:position :size :collision]
    (fn [dt subjs & without-subjs]
      (let [actor (first (filter (comp :actor? :collision) subjs))
            coll-subj (first (filter #(and (collided? actor %)
                                           (or (empty? without-subjs)
                                               (not (contains? (set without-subjs) %))))
                                     (disj (set subjs) actor)))]
        (if coll-subj
          (let [new-subjs (-> (set subjs)
                              (disj actor)
                              (disj coll-subj)
                              (into (collide actor coll-subj)))]
            (recur dt new-subjs (conj without-subjs coll-subj)))
          subjs)))]

   [;chase
    [:chase]
    (fn [dt subjs]
      (let [player (first (filter #(= :player (:type %)) subjs))
            enemy (first (filter #(= :enemy (:type %)) subjs))
            [disp-x _] (displacement (:position player) (:position enemy))]
        (if (neg? disp-x)
          [player (phys-v enemy [-1 0] -3 dt)]
          [player (phys-v enemy [ 1 0]  3 dt)])))]

   [[:self-destruct]
    (fn [dt subjs]
      (let [now (Date.now)]
        (remove (fn [{{:keys [after spawn-time]} :self-destruct}]
                  (< (+ spawn-time after) now))
             subjs)))]])




(defn process-evts [{:keys [evt-history] :as db} at-tick]
  (reduce (fn [acc-db [evt-id :as evt]]
            #_(l "EVT:" evt)
            ((get events evt-id) acc-db evt))
          db
          (get evt-history at-tick)))

(defn run-systems [db dt]
  (update db :entities (fn [ents] (reduce (fn [ents-acc [reqs sys-fn]]
                                            (let [subjs (filter (fn [ent] (every? #(get ent %) reqs)) ents-acc)
                                                  new-subjs (sys-fn dt subjs)]
                                              (-> (clojure.set/difference ents-acc subjs)
                                                  (into new-subjs))))
                                          ents systems))))

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
              (let [time-skewed (* 2 (reduce + (map (fn [[from till]] (- till from)) (:reverse-history @mnemo))))
                    prev-tick (:current-tick db)
                    current-tick (inc prev-tick)
                    ;now (- (js/Date.now) time-skewed)
                    ;prev-tick (or (:current-tick db) (- (js/Date.now) 1000))
                    dt dt ;(- now prev-tick)
                    ]
                (if (:current-reverse @mnemo)
                  (-> db
                      (back-tick)
                      (drop-controls))
                  (-> (assoc db :current-tick current-tick)
                      (process-evts prev-tick)
                      (run-systems dt)
                      (mnemo-tick db))))))
  )


(defonce sys-running (atom nil))
(defn clear-sys-running []
  (when-let [sr (l 111 @sys-running)]
    (js/clearInterval sr)))
(clear-sys-running)
(reset! sys-running (js/setInterval tick! 16))

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
   {:which 17
    :to :crouch
    :on-press? true}
   {:which 74
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
                             #_(.preventDefault e)
                             #_(.stopPropagation e)
                             (when to
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
                             #_(.preventDefault e)
                             #_(.stopPropagation e)
                             (when to
                               (if on-press?
                                 (onpress :up [:controls to])
                                 (deduple [:set-controls to 0])))))))))


(defmulti render :type)
(defmethod render :player
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render
    :keys [facing angle weapon id]}]
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
   ])

#_(defmethod render :bullet
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render}]
  [:image {:x x :y y :width w :height h
           :xlinkHref "/sandbox/bullet.png"}]
  #_[:rect {:x x :y y :width w :height h
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

(>evt [:add-ent ent-floor])
(doseq [stair ent-stairs]
  (>evt [:add-ent stair]))
(>evt [:add-ent ent-player])
(>evt [:add-ent ent-enemy])
(l "DB:" @db)

(defmethod panels/panel :frozen-in-time
  [_]
  [:div
   [:style (garden.core/css styles)]
   [:svg {:width "100vw"
          :height "100vh"
          :on-mouse-move #(when (normal-time?) (>evt [:mouse (coords %)]))
          :on-click #(do #_(.preventDefault %)
                         #_(.stopPropagation %)
                         (>evt [:fire]))
          }
    [:text {:x 10 :y 15}
     "WASD to move"]
    [:text {:x 10 :y 27}
     "Left mouse button to shoot"]
    [:text {:x 10 :y 40}
     "SPACE to rewind"]
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
    #_[:text {:x 200 :y 200} (str (:mouse @db))]
    (for [{:keys [id] :as ent} (sort-by :id (:entities @db))]
      ^{:key id}
      [render ent])
    #_[inspect (:controls @db) 10 30]
    ]])
