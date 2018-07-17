(ns brawl-haus.panels.race
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            [re-com.misc :as rcmisc]))

(defn current-race [db]
  (let [race-id (get-in db [:current-panel :route-params :race-id])]
    (get-in db [:public-state :open-races race-id])))

(rf/reg-sub
 :current-race
 (fn [db _]
   (current-race db)))

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

(def breaks #{\, \space \.})
(defn bite [text chunk]
  (when (and (not-empty text)
             (not-empty chunk)
             (clojure.string/starts-with? text chunk))
    (cond (= text chunk) ""
          (contains? breaks (last chunk)) (subs text (count chunk)))))

(defn how-matches [str substr]
  (count (take-while true? (map = str substr))))

(defn meta-text [{:keys [race-text] :as race} current-text left-text]
  (js/console.log race-text current-text left-text)
  (when race-text
    (let [[done after-done] (split-at (- (count race-text) (count left-text))
                                      race-text)
          [typed after-typed] (split-at (count current-text) left-text)

          [right-typed _] (split-at (how-matches after-done current-text) after-done)
          [wrong-typed after-wrong] (->> after-done
                                         (drop (count right-typed))
                                         (split-at (- (count current-text) (count right-typed))))
          ]
      (concat  (map (fn [x] {:char x :statuses #{"done"}}) done)
               (map (fn [x] {:char x :statuses #{"right-typed"}}) right-typed)
               (map (fn [x] {:char x :statuses #{"wrong-typed"}}) wrong-typed)
               (map (fn [x] {:char x :statuses #{"yet"}}) after-wrong)))
    ))

(defn race-progress [race]
  [:div.race-progress
   (doall
    (for [[id {:keys [speed left-chars]}] (:participants race)]
      (let [{:keys [nick]} (<sub [:user id])]
        [:div.participant {:key id}
         [:div.nick nick]
         (when speed
           [:span.average-speed.badge.white-text (str speed)])
         [rcmisc/progress-bar
          :striped? (not (zero? left-chars))
          :model (if left-chars
                   (- 100
                      (-> left-chars
                          (/ (count (:race-text race)))
                          (* 100)))
                   0)]
         ])))])

(defn waiting []
  (r/with-let [dots (r/atom 4)
               _ (js/setInterval #(swap! dots inc) 800)]
    [:div.card.waiting
     [:div.card-content "Waiting a company"
      (case (rem @dots 4)
            0 ""
            1 "."
            2 ".."
            3 "...")]]))

(defn text-race [race]
  (r/with-let [input-state (r/atom {:current-text ""
                                    :left-text (:race-text race)})
               on-type
               (fn [e]
                 (when (t/after? (t/now) (c/from-date (:starts-at race)))
                   (swap! input-state
                          (fn [{old-current-text :current-text
                                left-text :left-text :as old-state}]
                            (let [current-text (.-value (.-target e))]
                              (if-let [new-left-text (bite left-text current-text)]
                                (do (rf/dispatch [:tube/send [:left-text (:id race) new-left-text]])
                                    {:current-text ""
                                     :left-text new-left-text})
                                (assoc old-state :current-text current-text)))))))]
    (let [{:keys [current-text left-text]} @input-state]
      [:div.text-race.card
       [:div.race-text.card-content
        (doall
         (map-indexed
          (fn [idx {:keys [char statuses]}]
            ^{:key idx}
            [:div.char {:class (str (apply str statuses)
                                    (when (= " " char) " whitespace"))}
             (when (contains? statuses "wrong-typed")
               (.play (js/Audio. "audio/bump.mp3")) nil)
             char])
          (meta-text race current-text left-text)))]
       [:input {:on-change on-type
                :disabled (zero? (count left-text))
                :value current-text
                :ref #(when % (.focus %))}]
       ])))


(defn to-next []
  [:button.btn.btn-flat {:on-click #(rf/dispatch [:tube/send [:race/attend]])} "Next race"])

(rf/reg-sub
 :race
 (fn [db [_ race-id]]
   (get-in db [:public-state :open-races race-id])))

(defmethod panels/panel :race-panel
  [{:keys [params]}]
  (let [race (<sub [:race (:race-id params)])]
    [:div.app
     [:div.content.race-panel
      [countdown race]
      [text-race race]
      [race-progress race]
      [to-next]]]))
