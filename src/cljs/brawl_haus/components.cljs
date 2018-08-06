(ns brawl-haus.components
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [brawl-haus.focus :as focus]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [cljs-time.coerce :as c]))

(rf/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(rf/reg-sub
 :messages
 (fn [db _]
   (reverse
    (sort-by :received-at (get-in db [:public-state :messages])))))

(defn set-nick []
  (r/with-let [new-nick (r/atom "")]
    [:div.set-nick
     [:input.collection-header {:value @new-nick
                                :on-change #(reset! new-nick (l "VAL:" (.-value (.-target %))))
                                :placeholder (:nick (<sub [:user]))}]
     [:button.btn.btn-flat {:on-click #(do (rf/dispatch [:tube/send [:chat/set-nick @new-nick]])
                                           (reset! new-nick ""))} "Set nick"]]))

(defn participants []
  (let [users (->> (<sub [:users])
                   (sort-by :nick)
                   vals
                   (remove #(= :quit (get-in % [:location :location-id]))))]
    [:ul.collection.with-header.participants.z-depth-1
     [set-nick]
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
     [:input {:id :chat-input
              :value @input-msg
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
        (for [{:keys [id sender text received-at]} (<sub [:messages])]
          (let [{:keys [nick]} (<sub [:user sender])]
            [:li.collection-item {:key id
                                  :class (when (= sender
                                                  (:tube (<sub [:user]))) "my")}
             [:div.from nick]
             [:div.received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))]
             [:div.text text]])))]
      [:div.empty "No talking yet, be the first!"])]
   [participants]])


