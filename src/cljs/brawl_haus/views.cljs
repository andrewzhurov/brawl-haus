(ns brawl-haus.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [re-pressed.core :as rp]
   [brawl-haus.subs :as subs]
   ))

(defn <sub [evt] (deref (rf/subscribe evt)))

(defn l [desc expr] (js/console.log desc expr) expr)

(defn msg-input []
  (r/with-let [input-msg (r/atom "")]
    [:input {:on-change #(reset! input-msg (-> % .-target .-value))
             :on-key-down #(when (= (.-keyCode %) 13)
                             (rf/dispatch [:add-message (-> % .-target .-value)])
                             (reset! input-msg ""))
             :value @input-msg}
     ]))

(defn home-panel []
  [:div
   [:div.tube-indicator {:class (boolean @(rf/subscribe [:db/get-in [:tube]]))}]
   [:a {:href "#/race"} "To race!"]
   [:div.chat
    [:div.messages
     (for [message @(rf/subscribe [:db/get-in [:messages]])]
       [:div.message message])]
    [msg-input]]])


;; race-game-panel
  (def text "To be or not to be")

(def breaks #{\, \space \.})
(defn bite [text chunk]
  (when (and (not-empty chunk)
             (clojure.string/starts-with? text chunk))
    (cond (= text chunk) ""
          (contains? breaks (last chunk)) (subs text (count chunk)))))

(rf/reg-event-fx
 :current-text
 (fn [{:keys [db]} [_ current-text]]
   (let [left-text (:left-text db)]
     (merge
      {:db (assoc db :current-text current-text)}
      (when-let [new-left-text (bite left-text current-text)]
        {:db (assoc db
                    :current-text ""
                    :left-text new-left-text)
         :tube-send [:left-text new-left-text]})))))

(defn word-input []
  [:input {:on-change #(rf/dispatch [:current-text (-> % .-target .-value)])
           :value @(rf/subscribe [:db/get-in [:current-text]])}
   ])

(defn how-matches [str substr]
  (count (take-while true? (map = (l 11 str) (l 22 substr)))))

(rf/reg-sub
 :meta-text
 (fn [{:keys [left-text current-text whole-text]} _]
   (when whole-text
     (let [[done after-done] (split-at (- (count whole-text) (count left-text))
                                whole-text)
           [typed after-typed] (split-at (l "COUNT:" (count current-text)) left-text)

           [right-typed _] (l "RES:" (split-at (l "MATCHES:" (how-matches after-done current-text)) after-done))
           _ (l "RIGHT TYPED:" right-typed)
           [wrong-typed after-wrong] (->> after-done
                                         (drop (count right-typed))
                                         (split-at (- (count current-text) (count right-typed))))]
       (concat  (map (fn [x] {:char x :statuses #{"done"}}) done)
                (map (fn [x] {:char x :statuses #{"right-typed"}}) right-typed)
                (map (fn [x] {:char x :statuses #{"wrong-typed"}}) wrong-typed)
                (map (fn [x] {:char x :statuses #{"yet"}}) after-wrong)))
     )))

(defn race-text []
  [:div.race-text
   (map-indexed
    (fn [idx {:keys [char statuses]}]
      ^{:key idx}
      [:div.char {:class (apply str statuses)}
       char])
    (l "LEFT:" (<sub [:meta-text])))])

(defn ready-btn []
  [:button {:on-click #(do (rf/dispatch [:db/set-in [:whole-text] text])
                           (rf/dispatch [:db/set-in [:left-text] text]))} "Ready!"])
(defn race-game-panel []
  [:div.race-game
   [:div.tube-indicator {:class (boolean @(rf/subscribe [:db/get-in [:tube]]))}]
   [ready-btn]
   [race-text]
   [word-input]])

;; main
(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :race-game-panel [race-game-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (rf/subscribe [::subs/active-panel])]
    [re-com/v-box
     :height "100%"
     :children [[panels @active-panel]]]))
