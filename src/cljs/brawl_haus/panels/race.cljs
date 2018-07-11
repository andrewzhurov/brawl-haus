(ns brawl-haus.panels.race
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]))


(defn countdown [race]
  (r/with-let [now (r/atom (t/now))
               looper (js/setInterval #(reset! now (t/now)) 20)]
    (let [starts (c/from-date (:starts-at race))
          remains (t/in-millis
                   (cond
                     (nil? starts) (t/seconds 10)
                     (t/after? @now starts) (do (js/clearInterval looper)
                                                (t/seconds 0))
                     :counting (t/interval @now starts)))]
      [:div.countdown
       (str (int (/ remains 1000)) ":" (rem remains 1000))])))

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
       (when (contains? statuses "wrong-typed") ; Ha, loving the effect of amplification
         (.play (js/Audio. "audio/bump.mp3")) nil)
       char])
    (l "LEFT:" (<sub [:meta-text])))])

(defn ready-btn []
  [:button {:on-click #(do (rf/dispatch [:db/set-in [:whole-text] text])
                           (rf/dispatch [:db/set-in [:left-text] text]))} "Ready!"])

(rf/reg-sub
 :race
 (fn [db [_ race-id]]
   (get-in db [:public-state :open-races race-id])))

(defmethod panels/panel :race-panel
  [_ route-params]
  (l "Route params:" route-params)
  (let [race (<sub [:race (l "Race id:" (:race-id route-params))])]
    [:div.app
     [panels/navbar]
     [:div.content.race-panel
      [countdown race]
      [:div.tube-indicator {:class (boolean @(rf/subscribe [:db/get-in [:tube]]))}]
      [ready-btn]
      [race-text]
      [word-input]]]))
