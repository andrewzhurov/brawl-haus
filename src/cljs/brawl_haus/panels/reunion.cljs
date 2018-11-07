(ns brawl-haus.panels.reunion
  (:require [garden.core]
            [reagent.core :as r]
            [brawl-haus.panels :as panels]))

(defn l [mark expr] (js/console.log mark expr) expr)

(defn take-common
  "Takes common start part of both str"
  [str1 str2]
  (->> (map (fn [ch1 ch2] {:equal? (= ch1 ch2)
                           :ch ch1}) str1 str2)
       (take-while :equal?)
       (map :ch)
       (apply str)))

(def face-ids
  [[""  "" "" "one-l" "one" "one-r" "one-l2" "one-r2"]
   [""  "" "" "two-l" "two" "two-r" "two-l2" "two-r2"]
   [""  "" "" "three-l" "three" "three-r" "three-l2" "three-r2"]
   [""  "" "" "four-r" "four" "four-l" "four-l2" "four-r2"]
   [""  "" "" "five-r" "five" "five-l" "five-l2" "five-r2"]
   ["six" "" "" ""   ""  ""   "" ""]])

(def sprites-width 52.4)
(def sprites-height 66)

(defn px [num] (str num "px"))
(def style
  [[:#content {:margin-top "5px"
               :display "flex"
               :flex-direction "column"
               :align-items "center"}]
   [:.enemies {:width "400px"}
    [:.beaten {:background-color "lightgray"}]]
   [:.face-group {:cursor "pointer"
                  :margin "auto"}
    [:.dots {:text-align "center"}]
    [:.reply {:visibility "hidden"}
     [:.text {:visibility "hidden"
              :text-align "center"}]]
    [:&.has-to-say
     [:.reply {:visibility "visible"}]]
    [:.face {:background "url('brawl/sprites/face.png')"
             :margin "auto"
             :width (px sprites-width)
             :height (px sprites-height)}
     (map-indexed (fn [row-idx row]
                    (map-indexed (fn [column-idx id]
                                   (when (not (empty? id))
                                     (let [top (* row-idx sprites-height)
                                           left (* column-idx sprites-width)
                                           left (if (= column-idx 7)
                                                  (+ left 13)
                                                  left)]
                                       [(keyword (str "&." id)) {:background-position (str "-" (px left) " -" (px top))}])))
                                 row))
                  face-ids)
     ]
    [:&.has-to-say:hover
     [:.dots {:visibility "hidden"}]
     [:.text {:visibility "visible"
              :font-weight "600"
              :text-decoration "underline"}]
     [:.face {:background-position-x "0px"}]
     [:.face.six {:background-position-y (px (- (* 4 sprites-height)))}]
     ]]])

(def picks
  [{:pick "You're dead!"
    :reply "Wrong."}
   {:pick "I severed your head from your shoulders!"
    :reply "And was it enough?"}
   {:pick "Killed you!"
    :reply "But here we are."}
   {:pick "The ones you lost are not coming back!"
    :reply "And so do you."}
   {:pick "May have beaten me, but Reign will prevail."
    :reply "Feels like a good start."}])

(def enemies-data
  (into {}
        (map (fn [x]
               (let [id (random-uuid)]
                 [id (merge x
                            {:id id
                             :beaten? false})])))
        picks))

(def init-state {:enemies enemies-data
                 :target nil
                 :level 1
                 :face "one"
                 :reply "none"
                 :done nil})

(def state (r/atom init-state))

(defn play [sound] (.play (js/Audio. (str "brawl/audio/" sound ".mp3"))))

(defn enemies [{:keys [on-select on-hit on-miss on-beat on-win]}]
  [:div
   [:div.enemies.collection
    (for [[id {:keys [pick beaten?]}] (:enemies @state)]
      [:a.collection-item {:id id
                           :class (str (when (= id (:target @state)) " active")
                                       (when beaten? "beaten"))}
       pick])]
   [:text "Your last words:"]
   [:input {:on-change (fn [e]
                         (let [curr (.-value (.-target e))]
                           (swap! state
                                  (fn [{:keys [enemies target done] :as curr-state}]
                                    (let [target-pick (get-in enemies [target :pick])]
                                      (if (nil? target)
                                        (let [enemy-select
                                              (some (fn [{:keys [id pick beaten?] :as all}]
                                                      (let [common (take-common curr pick)]
                                                        (when (and (not beaten?)
                                                                   (not (empty? common)))
                                                          {:target id
                                                           :done common})))
                                                    (vals enemies))]
                                          (if enemy-select
                                            (on-hit (merge curr-state
                                                           enemy-select))
                                            curr-state))

                                        (let [new-done (take-common curr target-pick)
                                              ;; Effects
                                              new-state ((cond
                                                           (= target-pick new-done)
                                                           on-beat
                                                           (= new-done done)
                                                           on-miss
                                                           :else
                                                           on-hit)
                                                         curr-state)
                                              new-state (if (= target-pick new-done)
                                                          (-> new-state
                                                              (assoc-in [:enemies target :beaten?] true)
                                                              (assoc :target nil)
                                                              (assoc :done nil))

                                                          (assoc new-state :done new-done))]
                                          (when (every? :beaten? (vals (:enemies new-state)))
                                            (on-win))
                                          new-state)))))))
            :value (:done @state)}]])


(def levels
  {1 "one"
   2 "two"
   3 "three"
   4 "four"
   5 "five"
   6 "six"})

(defmethod panels/panel :reunion-panel
  []
  [:div#content
   [:style (garden.core/css style)]
   [:div.face-group (when (and (> (:level @state) 4)
                               (empty? (:done @state))) "ready"
                          {:class "has-to-say"
                           :on-click #(reset! state init-state)})
    [:div.face {:class (:face @state)}]
    [:div.reply
     [:div.dots "..."]
     [:div.text (:reply @state)]]]
   [enemies {:on-select #(println "YOU!")
             :on-hit (fn [{:keys [level position] :as state}]
                       (merge state
                              (let [move (rand-nth (vec (disj #{:left :center :right} position)))]
                                (case move
                                  :left (do (play "left_hook")
                                            {:position :left
                                             :face (str (levels level) "-r" (rand-nth ["" 2]))})
                                  :center (do (play "jab")
                                              {:position :center
                                               :face (levels level)})
                                  :right (do (play "right_hook")
                                             {:position :right
                                              :face (str (levels level) "-l" (rand-nth ["" 2]))})))))
             :on-miss (fn [state] (play "whoosh") state)
             :on-beat (fn [{:keys [level target] :as state}]
                        (play "upper_cut")
                        (let [new-level (inc level)]
                          (l "STATE:" state)
                          (merge state
                                 {:level new-level
                                  :position :center
                                  :face (levels new-level)
                                  :reply (l 111 (get-in state [:enemies target :reply]))})))
             :on-win (fn [state] (play "cheering") state)}]])
