(ns brawl-haus.sandbox
  (:require [garden.core]
            [reagent.core :as r]
            [reagent.ratom :as ra]
            [re-frame.core :refer [reg-event-db reg-sub reg-sub-raw]]
            [brawl-haus.utils :refer [l <sub >evt]]
            [clojure.data :refer [diff]]
            ))
(def field-width 1000) ;m
(def field-height 600) ;m
(def node-width js/window.innerWidth)
(def node-height js/window.innerHeight)

(def ship2 (js/Image.))
(set! (.-src ship2)  "/sandbox/ship.png")
(def player-resources
  {0 {:ship-ff (js/Audio. "/sandbox/ship-ff.wav")
      :ship-ff-depleted (js/Audio. "/sandbox/ship-ff-depleted.wav")}
   1 {:ship-ff (js/Audio. "/sandbox/ship-ff.wav")
      :ship-ff-depleted (js/Audio. "/sandbox/ship-ff-depleted.wav")}
   2 {:ship-ff (js/Audio. "/sandbox/ship-ff.wav")
      :ship-ff-depleted (js/Audio. "/sandbox/ship-ff-depleted.wav")}
   3 {:ship-ff (js/Audio. "/sandbox/ship-ff.wav")
      :ship-ff-depleted (js/Audio. "/sandbox/ship-ff-depleted.wav")}})

(defn stop [js-audio]
  (.pause js-audio)
  (set! (.-current-time js-audio) 0))

(defn pplay [player-id audio-id]
  (.play (get-in player-resources [player-id audio-id])))
(defn pstop [player-id audio-id]
  (stop (get-in player-resources [player-id audio-id])))

(defn player-id [ent]
  (get-in ent [:gp-controlled :gp-idx]))



(defn scale-x [x] (* (/ field-width node-width) x))
(defn scale-y [y] (* (/ field-height node-height) y))

(declare entities)

(def home-path "/sandbox")
(def coin-mods ["a6" "c6" "c7" "d6" "f6" "g6" "h6"])
(defn coin-sound [mod]
  (str home-path "/fins_scale-" mod ".wav"))
(defn random-coin-play []
  (.play (js/Audio. (coin-sound (rand-nth coin-mods)))))
(defn play [file]
  (.play (js/Audio. (str home-path file))))

(def styles
  [[:#CANVAS {:width "100vw"
              :height "100vh"}]
   [:svg#root {:height "100vh"
               :width "100vw"}]
   [:.joystick {:x "200px" :y "200px"}
    [:.bound {:r "100px"
              :fill "transparent"
              :stroke-width "2px"
              :stroke "gray"}]
    [:.center {:r "10px"
               :fill "transparent"
               :stroke-width "2px"
               :stroke "lightgray"}]
    [:.position {:r "5px"}]]
   [:.coin-spawner {:fill "gold"
                    :fill-opacity "0.5"
                    :stroke "gold"
                    :stroke-width "5px"}]
   [:.coin-hud
    [:text {:font-size "40px"}]]])

(def fps 60)
;; ms
(def dt (/ 1000 fps))
(defn per-n-s [n]
  (fn [x]
    (/ x fps n)))

(def tick (r/atom (js/Date.now)))
(defonce ticker (js/setInterval #(reset! tick (js/Date.now)) dt))

(reg-sub-raw :tick
             (fn []
               (ra/reaction @tick)))

(defn update-set [s pred update-fn]
  (let [the-vals (filter pred s)]
    (-> s
        (clojure.set/difference the-vals)
        (into (map update-fn the-vals)))))

;; Components
(defn comp-position [ent init]
  (merge ent {:position init}))



(defn knock [ent velocity]
  (update-in ent [:motion :velocity] #(map (fn [v1 v2] (+ v1 v2)) % velocity)))


(defn conjv [v? el]
  (if v? (conj v? el) []))
(defn add-beh [ent beh]
  (update ent :beh conjv beh))



(defn shift [[x1 y1] [x2 y2]]
  [(- x1 x2) (- y1 y2)])
(defn hypotenuse [[a b]]
  (Math/sqrt (+ (Math/pow a 2) (Math/pow b 2))))
(defn calc-angle [[x y]]
  (* (js/Math.atan2 y x) (/ 180 js/Math.PI)))
(defn right-axis [[x y]]
  [(-> x (- 0.5) (* 2))
   (-> y (- 0.5) (* 2))])
(defn standard-coords-system [[x y]]
  [x (* y -1)])

(defn in-rads [degrees]
  (* degrees (/ js/Math.PI 180)))



(def right-stick-treshold 0.4)
(defn sys-gp-controlled
  [ent]
  (let [gp (<sub [:gamepad (get-in ent [:gp-controlled :gp-idx])])
        new-acceleration (mapv #(* % 0.2) (get-in gp [:axes :left]))
        right-axis  (get-in gp [:axes :right])
        new-angle? (when (-> right-axis (hypotenuse) (js/Math.abs) (> right-stick-treshold)) ;; Treshold
                     (calc-angle (standard-coords-system right-axis)))]
    (-> ent
        (assoc-in [:motion :acceleration] new-acceleration)
        (update :angle #(or new-angle? %))
        (update :vec-angle #(if new-angle? right-axis %))
        )))

(defn comp-gp-controlled [ent gp]
  (-> ent
      (assoc :id (.-index gp) #_(.-id gp))
      (assoc :gp-controlled {:gp-idx (.-index gp)
                             :binds {:R1 :top-right
                                     :L1 :top-left}})
      (update :behs conj sys-gp-controlled)))


(defn comp-collision [ent r actor?]
  (merge ent {:collision {:r r
                          :actor? actor?}}))

(defn comp-with-money [ent]
  (assoc ent :money 0))
(defn money-gain [ent amount]
  (update ent :money + amount))
(defn money-deduce [ent amount]
  (update ent :money - amount))
(defn money-amount [ent]
  (get ent :money))


(def central-coin-spawner
  (-> {:id :central-coin-zone
       :type :coin-spawner
       :coin-spawner {:zone-size [500 300]
                      :every 3000
                      :last-at nil}}
      (comp-position [900 350])))




(defn comp-health [ent amount]
  (merge ent {:max-health amount
              :health amount}))
(defn health-damage [ent amount]
  (update ent :health - amount))
(defn health-status [ent]
  (:health ent))

(defn comp-self-destruct [ent]
  (update ent :behs conj (fn [ent] (if (< (hypotenuse (get-in ent [:motion :velocity])) 0.2)
                                     {}
                                     ent))))

(defn comp-repel-equip [ent]
  (assoc-in ent [:weapons :top-left] {:type :repel-field
                                      :x-force (fn [dx] (- (* dx 0.001)))
                                      :y-force (fn [dy] (- (* dy 0.001)))

                                      :before-recharge 3000
                                      :recharge-time 2000
                                      :max-capacity 180
                                      :current-capacity 20
                                      :last-use-at nil}))

(defn comp-weapons [ent]
  (merge ent {:weapons {:top-left nil
                        :top-right nil
                        :bottom-left nil
                        :bottom-right nil}}))

 ;; ms

(def slug-weapon-specs
  {1 {:level 1
      :damage 20
      :velocity-multiplier 1
      :cooldown 1000}
   2 {:level 2
      :damage 25
      :velocity-multiplier 1.33
      :cooldown 700}
   3 {:level 3
      :damage 20
      :velocity-multiplier 1.66
      :cooldown 333}
   4 {:level 4
      :damage 25
      :velocity-multiplier 1.77
      :cooldown 200}
   5 {:level 5
      :damage 20
      :velocity-multiplier 1.90
      :cooldown 120}
   })
(defn slug-add [ent]
  (assoc-in ent [:weapons :top-right] (get slug-weapon-specs 1)))

(defn try-upgrade [ent]
  (let [current-level (get-in ent [:weapons :top-right :level])
        enough-money? (>= (money-amount ent) 5)]
    (cond (= 5 current-level)
          (do (println "MAX LEVEL")
              ent)

          (not enough-money?)
          (do (println "No enough money")
              ent)

          :deal!
          (do (play "/weapon_up.wav")
            (-> ent
                (assoc-in [:weapons :top-right] (get slug-weapon-specs (inc current-level)))
                (money-deduce 5))))))

(defn weapon-ready? [ent slot at]
  (when (:weapons ent)
    (let [{:keys [last-fired-at cooldown]} (get-in ent [:weapons slot])]
      (or (nil? last-fired-at)
          (> (- at last-fired-at)
             cooldown)))))

(defn triggered-weapons [ent]
  (when-let [gp-idx (get-in ent [:gp-controlled :gp-idx])]
    (->> (:buttons (<sub [:gamepad gp-idx]))
         (map (fn [btn]
                (and (not= (:value btn) 0)
                     (#{:R1 :L1} (:id btn)))))
         (remove false?))))

(declare slug)
(defn fire-slug [ent]
  (let [at (js/Date.now)
        ship-velocity (get-in ent [:motion :velocity])
        slug-velocity (->> (:vec-angle ent)
                           (mapv #(* % 20 (get-in ent [:weapons :top-right :velocity-multiplier]))))
        slug-position (mapv #(+ % (* 3 %2)) (:position ent) slug-velocity)
        slug-angle (+ 90 (calc-angle (standard-coords-system slug-velocity)))
        slug-velocity (->> slug-velocity
                           (mapv + ship-velocity))
        weapon-damage (get-in ent [:weapons :top-right :damage])]
    (.play (js/Audio. "/sandbox/tank_cannon.mp3"))
    [(assoc-in ent [:weapons :top-right :last-fired-at] at)
     (slug weapon-damage slug-position slug-velocity slug-angle)]))



(declare comp-position)
(declare comp-force)
(defn fire-field [ent]
  (let [r 150
        p-id (player-id ent)]
    (if (> (get-in ent [:weapons :top-left :current-capacity]) 0)
      (do (pstop p-id :ship-ff-depleted)
          (pplay p-id :ship-ff)
          [(-> ent
               (update-in [:weapons :top-left :current-capacity] dec)
               (assoc-in [:weapons :top-left :last-use-at] (js/Date.now)))
           (-> {:id (random-uuid)
                :type :ship-force-field
                :temp-force? true}
               (comp-position (:position ent))
               (comp-force {:force (fn [[dx dy]]
                                     (when (< (hypotenuse [(js/Math.abs dx) (js/Math.abs dy)]) r)
                                       (map (fn [coord] (- (* 0.03 coord))) [dx dy])))
                            :size (* r 2)}))])
      (do (pstop p-id :ship-ff)
          (pplay p-id :ship-ff-depleted)
          [ent]))))


(defn comp-render [ent texture size]
  (merge ent {:render {:texture texture
                       :size size}}))




;; Gamepad API

(def num->btn
  {0 :circle
   1 :triangle
   2 :cross
   3 :rectangle
   4 :L1
   5 :R1
   ;6 :raxis-x
   ;7 :raxis-y
   8 :share
   9 :options
   10 :P
   11 :L3
   12 :dpad-top
   13 :dpad-bottom
   14 :dpad-left
   15 :dpad-right
   16 :R3
   ;17 ??
   })
;;    1
;; -1   1
;;   -1

(defn gamepad [idx]
  (when-let [gp (aget (js/navigator.getGamepads) idx)]
    (let [right-axis (right-axis [(aget gp "buttons" 6 "value")
                                  (aget gp "buttons" 7 "value")])
          left-axis [(aget gp "axes" 0) (aget gp "axes" 1)]]
      {:id (.-id gp)
       :buttons (->>
                 (aget gp "buttons")
                 (map-indexed (fn [idx x] {:id (num->btn idx)
                                           :pressed (.-pressed x)
                                           :value (.-value x)}))
                 (filter :id))
       :axes {:left left-axis
              :right right-axis}
       }
      )))

(reg-sub :gamepad
         :<- [:tick]
         (fn [_ [_ gp-idx]]
           (gamepad gp-idx)))

;; Systems
;; MOTION
(defn sys-motion
  [{:keys [position]
    {:keys [velocity acceleration]} :motion
    :as ent}]
  (let [new-velocity (mapv #(-> (+ %1 %2) (* 0.97)) velocity acceleration)] ;; degrade
    (-> ent
        (assoc-in [:motion :velocity] new-velocity)
        (update :position #(mapv + % new-velocity)))))

(defn comp-motion [ent & [init-velocity]]
  (-> ent
      (assoc :motion {:velocity (or init-velocity [0 0])
                      :acceleration [0 0]})
      (update :behs conj sys-motion)))

;; 


;; COIN




;; COLLISION
(defn collided?
  [{position1 :position
    collision1 :collision :as ent1}
   {position2 :position
    collision2 :collision :as ent2}]
  (and (not= ent1 ent2)
       (> (+ (:r collision1) (:r collision2))
          (hypotenuse (shift position1 position2)))))
(defmulti collide (fn [act ent] [(:type act) (:type ent)]))

(defmethod collide [:ship :coin]
  [ship {:keys [value]}]
  (random-coin-play)
  [(-> ship
       (money-gain value)
       (try-upgrade))])

(defmethod collide [:slug :slug]
  [_ _]
  (play "/CasualGameSounds/DM-CGS-48.wav")
  [])

(defmethod collide [:ship :slug]
  [ship slug]
  (let [{:keys [damage]
         {:keys [velocity]} :motion} slug]
    (play "/CasualGameSounds/DM-CGS-49.wav")
    [(-> ship
         (health-damage damage)
         (knock velocity))]))

(defmethod collide [:slug :ship]
  [slug ship]
  (let [{:keys [damage]
         {:keys [velocity]} :motion} slug]
    (play "/CasualGameSounds/DM-CGS-49.wav")
    [(-> ship
         (health-damage damage)
         (knock velocity))]))

(defmethod collide :default
  [ent1 ent2]
  [ent1 ent2])

(defn sys-collision [ents]
  (let [{actors true
         passive false
         others nil} (group-by #(get-in % [:collision :actor?]) ents)
        subjects (into actors passive)
        [act subj] (loop [[act & acts :as all-acts] actors
                          [subj & subjs :as s] subjects]
                     (cond (nil? act)
                           nil

                           (nil? subj)
                           (recur acts subjects)

                           (= act subj)
                           (recur all-acts subjs)

                           (collided? act subj)
                           (do #_(println "COLLIDED:" act subj)
                               [act subj])

                           :not
                           (recur all-acts subjs)))]
    (if (and act subj)
      (into (disj ents act subj) (collide act subj))
      ents)))



;; RENDER
(def canvas-ctx (atom nil))
(defn circle [[x y] r]
  (.beginPath @canvas-ctx)
  (.arc @canvas-ctx x y r 0 (* 2 js/Math.PI))
  (.stroke @canvas-ctx)
  (.fill @canvas-ctx))

(defmulti render-ent :type)

(defn health-bar [{{:keys [size]} :render
                   [x y] :position
                   :keys [max-health health]}]
  (let [ctx @canvas-ctx
        per-p (/ size max-health)
        left (* (/ health max-health)  size)
        gone (* (- 1 (/ health max-health)) size)
        h 20]
    (.translate ctx (- x (/ size 2)) (- y (/ size 2))
                )
    (.beginPath ctx)
    (set! (.-fillStyle ctx) "green")
    (.rect ctx 0 -30 left h)
    (.fill ctx)

    (.beginPath ctx)
    (set! (.-fillStyle ctx) "red")
    (.rect ctx left -30 gone h)
    (.fill ctx)

    (.setTransform ctx 1 0 0 1 0 0)
    ))

(defn ff-bar [{{:keys [size]} :render
               [x y] :position
               {{:keys [current-capacity max-capacity]} :top-left} :weapons}]

  (let [ctx @canvas-ctx
        per-p (/ size max-capacity)
        left (* (/ current-capacity max-capacity)  size)
        gone (* (- 1 (/ current-capacity max-capacity)) size)
        w 20]
    (.translate ctx (- x (/ size 2)) (- y (/ size 2)))

    (.beginPath ctx)
    (set! (.-fillStyle ctx) "blue")
    (.rect ctx -30 0 w left)
    (.fill ctx)

    (when (= 0 gone)
      (.stroke ctx))

    (.setTransform ctx 1 0 0 1 0 0)
    ))


(defmethod render-ent :ship
  [{:keys [angle]
    [x y] :position
    {:keys [texture size]} :render :as ent}]
  (let [ctx @canvas-ctx
        angle (+ (- angle) 90)
        rad-angle (in-rads angle)
        cx (- x (/ size 2))
        cy (- y (/ size 2))
        ]
    #_(.translate ctx (/ size 2) (/ size 2))
    #_(.rotate ctx rad-angle)
    #_(set! (.-fillStyle @canvas-ctx) "rgb(200, 0, 0)")
    #_(circle [x y] (/ size 2))
    (.fill @canvas-ctx)
    (.translate ctx x y)
    (.rotate ctx rad-angle)
    (.drawImage ctx ship2 (- (/ size 2)) (- (/ size 2)) size size)
    (.setTransform ctx 1 0 0 1 0 0)
    (health-bar ent)
    (ff-bar ent)
    ))

(defmethod render-ent :coin
  [{:keys [position]
    {:keys [texture size]} :render}]
  (set! (.-fillStyle @canvas-ctx) "rgb(255, 215, 0)")
  (circle position (/ size 2))
  (.fill @canvas-ctx))

(defmethod render-ent :slug
  [{:keys [position]
    {:keys [texture size]} :render}]
  (set! (.-fillStyle @canvas-ctx) "rgb(255,140,0)")
  (circle position (/ size 2))
  (.fill @canvas-ctx))

(defmethod render-ent :ship-force-field
  [{[x y] :position
    {:keys [size]} :force}]
  (set! (.-fillStyle @canvas-ctx) "rgba(51, 153, 255, 0.2)")
  (circle [x y] (/ size 2))
  )

(defmethod render-ent :default
  [{[x y] :position
    {:keys [texture size]} :render :as ent}]
  #_(set! (.-fillStyle @canvas-ctx) "rgb(150, 150, 80)")
  #_(.fillRect @canvas-ctx x y size size))

(defn sys-canvas-render [ents]
  (if-let [ctx @canvas-ctx]
    (do (.clearRect ctx 0 0 node-width node-height)
        (doseq [{:keys [position] :as ent} ents
                :when position]
          (render-ent ent)))
    (println "NO CANVAS"))
  ents)



;; FORCE
(defn comp-force [ent {:keys [force force-x force-y size]}]
  (-> ent
      (merge {:force {:force-x force-x
                      :force-y force-y
                      :force force
                      :size size}})))

(defn sys-force [ents]
  (let [forces (filter :force ents)]
    (reduce (fn [acc-ents {[center-x center-y] :position
                           {:keys [force-x force-y force]} :force}]
              (reduce (fn [acc-ents2
                           {[x y] :position
                            :keys [motion] :as ent}]
                        (if (:motion ent)
                          (if force-y ;; assymetric, force-x + force-y
                            (let [v-x (force-x (- center-x x))
                                  v-y (force-y (- center-y y))]
                              (conj acc-ents2 (update-in ent [:motion :velocity] (fn [old] (mapv + old [v-x v-y])))))
                            ;; symmetric
                            (let [[v-x v-y] (force [(- center-x x) (- center-y y)])]
                              (conj acc-ents2 (update-in ent [:motion :velocity] (fn [old] (mapv + old [v-x v-y]))))))
                          (conj acc-ents2 ent)
                          ))
                      #{} acc-ents))
            ents forces)))



;;
(declare coin)
(def systems
  (array-map
   :equip
   (fn [ents]
     (reduce (fn [acc-ents ent]
               (let [tws (remove nil? (triggered-weapons ent))
                     tw (first tws)]
                 (if (empty? tws)
                   (do (when-let [p-id (player-id ent)]
                         (pstop p-id :ship-ff))
                       (conj acc-ents ent))
                   (cond (and (= :R1 tw) (weapon-ready? ent :top-right (js/Date.now)))
                         (into acc-ents (fire-slug ent))

                         (= :L1 tw)
                         (into (disj acc-ents ent) (fire-field ent))

                         :not-assigned
                         (conj acc-ents ent))
                   #_(reduce (fn [acc-ents2 tw]
                             )
                           acc-ents tws))))
             #{} ents))


   :force
   sys-force

   :collision
   sys-collision

   :canvas-render
   sys-canvas-render

   :temp-force-destruct
   (fn [ents]
     (into #{} (remove :temp-force? ents)))

   :gold-spawner
   (fn [ents]
     (let [{{[x-size y-size] :zone-size
             :keys [every last-at]} :coin-spawner
            [x y] :position
            :as ent} (first (filter #(= :coin-spawner (:type %)) ents))
           now (js/Date.now)
           ready? (or (nil? last-at) (< (+ last-at every) now))]
       (if ready?
         (-> ents
             (disj ent)
             (conj (assoc-in ent [:coin-spawner :last-at] now))
             (conj (coin [(+ (rand-int x-size) x)
                          (+ (rand-int y-size) y)])))
         ents)))

   :make-dead
   (fn [ents]
     (into #{} (remove #(and (:health %) (<= (:health %) 0)) ents)))

   :ff-charge
   (fn [ents]
     (into #{} (map (fn [ent]
                      (let [now (js/Date.now)]
                        (if-let [{:keys [current-capacity max-capacity last-use-at before-recharge]} (get-in ent [:weapons :top-left])]
                          (if (and (< current-capacity max-capacity)
                                   (or (nil? last-use-at) (< (+ last-use-at before-recharge) now)))
                            (update-in ent [:weapons :top-left :current-capacity] inc)
                            ent)
                          ent)))
                    ents)))

   ))



;; Entities
(def ship (-> {:type :ship}
              (comp-position [500 500])
              (comp-render "/sandbox/ship.svg" 60)
              (comp-motion)
              (comp-collision 30 true)
              (comp-weapons)
              (slug-add)
              (comp-health 160)
              (comp-repel-equip)
              ))

(def central-force (-> {:id (random-uuid)}
                       (comp-position [(/ node-width 2) (/ node-height 2)])
                       (comp-force {:force-x (fn [dx] (when (> (js/Math.abs dx) (- (/ node-width 2) 100))
                                                        (* 0.01 dx)))
                                    :force-y (fn [dy] (when (> (js/Math.abs dy) (- (/ node-height 2) 100))
                                                        (* 0.01 dy)))})))

(defn slug [damage position velocity angle]
  (-> {:id (random-uuid)
       :type :slug
       :damage damage
       :angle angle}
      (comp-position position)
      (comp-motion velocity)
      (comp-render "/sandbox/slug.svg" 18)
      (comp-collision 9 true)
      (comp-self-destruct)))

(defn coin [position]
  (let [value (inc (rand-int 3))
        size (* value 10)]
    (-> {:id (random-uuid)
         :type :coin
         :value value}
        (comp-position position)
        (comp-render "/sandbox/coin.svg" size)
        (comp-collision (/ size 2) false))))

(defonce entities
  (r/atom #{central-force (coin [600 600]) central-coin-spawner}))

(defn reg-entity [ent]
  (swap! entities conj ent)
  (println "REGGING:" ent))

(defn unreg-entity-by [by]
  (println "UNREGGING BY:" by)
  (swap! entities
         (fn [ents]
           (->> ents
                (remove #(let [[unique-in-diff _ _] (diff by %)]
                           (nil? unique-in-diff)))
                (into #{})))))

;;
(defn gp-connect    [e] (reg-entity (comp-gp-controlled ship (l "CONN:" (.-gamepad e)))))
(defn gp-disconnect [e] (unreg-entity-by (comp-gp-controlled {} (.-gamepad e))))

(js/removeEventListener "gamepadconnected" gp-connect)
(js/removeEventListener "gamepaddisconnected" gp-disconnect)
(js/addEventListener "gamepadconnected" gp-connect)
(js/addEventListener "gamepaddisconnected" gp-disconnect)


;; Engine
(reg-sub :engine
         :<- [:tick]
         (fn [_ _]
           (swap! entities
                  (fn [ents]
                    (->>
                     (reduce (fn [acc-ents [id system-fn]]
                               (system-fn acc-ents))
                             ents systems)
                     (map (fn [{:keys [behs] :as ent}]
                            (if (not-empty behs)
                              (reduce (fn [acc-ent beh] (beh acc-ent)) ent behs)
                              ent)))
                     (into #{}))))))

(defn engine []
  [:g (do (<sub [:engine])
          nil)])



(defn coin-hud []
  (let [players-money (->> @entities
                           (filter #(= :ship (:type %)))
                           (map money-amount))]
    [:g.coin-hud
     (when-let [fpm (first players-money)]
       [:text {:x 10 :y 50} fpm])
     (when-let [spm (second players-money)]
       [:text {:x 1000 :y 50} spm])]))

(defn content []
  [:div
   [:style (garden.core/css styles)]
   [:canvas#CANVAS {:ref (fn [node]
                           (if node
                             (let [ctx (.getContext node "2d")]
                               (set! (.-width (.-canvas ctx)) node-width)
                               (set! (.-height (.-canvas ctx)) node-height)
                               (reset! canvas-ctx ctx))
                             (println "NO REF"))
                           )}]
   [:div.assets
    [:image#SHIP {:style {:top 0
                          :left 0
                          :height "60px"
                          :width "60px"
                          :position "absolute"
                          :z-index 100
                          :background-image "url(/sandbox/ship.png)"
                          }
                                        ;:src "/sandbox/ship.png"
                                        ;:xlinkHref "/sandbox/ship.png"
                  }]]
   [engine]])
