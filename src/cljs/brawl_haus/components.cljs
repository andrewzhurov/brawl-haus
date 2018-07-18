(ns brawl-haus.components
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [cljs-time.coerce :as c]
            ))

(rf/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(rf/reg-sub
 :messages
 (fn [db _]
   (reverse
    (sort-by :received-at (get-in db [:public-state :messages])))))

(defn participants []
  (let [users (->> (<sub [:users])
                   (sort-by :nick )
                   vals
                   (remove #(= :quit (get-in % [:location :location-id]))))]
    [:ul.collection.participants.z-depth-1
     (for [{:keys [id nick tube]} users]
       [:li.collection-item {:key nick
                             :style {:color "#26a69a"}}
        [:span.activity-indicator.badge.white-text.teal "on"]
        [:div.nick nick]])]))

(defn send-box []
  (r/with-let [input-msg (r/atom "")
               send-fn (fn [text]
                         (rf/dispatch [:tube/send [:add-message text]])
                         (reset! input-msg ""))]
    [:div.send-box
     [:input {:value @input-msg
              :ref #(when % (.focus %))
              :on-change #(reset! input-msg (.-value (.-target %)))
              :on-key-down #(when (= (.-keyCode %) 13)
                              (send-fn @input-msg))}]
     [:div.btn.btn-flat {:class (when (empty? @input-msg) "disabled")
                         :on-click #(send-fn @input-msg)}
      [:i.material-icons "send"]]]))

(defn chat []
  [:div.chat.card.z-depth-1 {:class (when (<sub [:db/get-in [:is-chat-open]]) "open")}
   [send-box]
   [:div.messages
    (if-let [messages (not-empty (<sub [:messages]))]
      [:ul
       (doall
        (for [{:keys [id text from received-at]} (<sub [:messages])]
          [:li.collection-item {:key id
                                :class (when (= (:id from) (:id (<sub [:user]))) "my")}
           [:div.from (:nick from)]
           [:div.received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))]
           [:div.text text]]))]
      [:div.empty "No talking yet, be the first!"])]
   [participants]
   ])


