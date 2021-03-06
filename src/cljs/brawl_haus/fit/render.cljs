(ns brawl-haus.fit.render
  (:require [brawl-haus.fit.state :as state]))

(defn tutorial []
  [:g
   [:text {:x 10 :y 15}
    "WASD to move"]
   [:text {:x 10 :y 27}
    "Left mouse button to shoot"]
   [:text {:x 10 :y 40}
    "SPACE to rewind"]])

(defmulti render :type)
(defmethod render :player
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render
    :keys [weapon id]}]
  (let [{:keys [facing angle]} @state/angle]
    [:g {:transform (str "translate(" x "," y ")"
                         (when (= :right facing) "scale(-1, 1)"))}
     [:rect {:x 0 :y 0 :width w :height h
             :fill color}]
     [:circle {:r 5 :color "yellow"}]
     [:g {:x x
          :y y
          :transform (str "translate(" -30 "," -0 ")"
                          "rotate(" angle "," 32 "," 10 ")"
                          )}

      [:image {:xlinkHref (:texture weapon)
               :width 40
               :height 20
               }]

      [:line {:stroke-width "1px"
              :stroke "url(#grad1)"
              :x1 2 :y1 7
              :x2 -500 :y2 6
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

(defmethod render :enemy
  [{[x y] :position
    [w h] :size
    {:keys [state]} :body
    {:keys [color]} :render
    {[px py] :at} :watcher
    }]
  [:g
   [:rect {:x x :y y :width w :height h
           :fill (case state
                   :normal "gray"
                   :stagger "orange"
                   :stun "blue")}]
   (when px
     [:line {:x1 (- x 10) :y1 (+ y 10)
             :x2 px :y2 (+ py 5)
             :stroke "url(#grad2)"
             :stroke-width "1px"}])])



(defmethod render :default
  [{[x y] :position
    [w h] :size
    {:keys [color]} :render}]
  [:rect {:x x :y y :width w :height h
          :fill "transparent" #_color}])
