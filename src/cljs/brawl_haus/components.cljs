(ns brawl-haus.components
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub defview view]]
            [brawl-haus.focus :as focus]))

(rf/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(rf/reg-sub
 :messages
 (fn [db _]
   (reverse
    (sort-by :received-at (get-in db [:public-state :messages])))))

(defview :set-nick
  (fn [{:keys [current-nick]}]
    (r/with-let [new-nick (r/atom "")]
      [:div.set-nick
       [:input.collection-header {:value @new-nick
                                  :on-change #(reset! new-nick (l "VAL:" (.-value (.-target %))))
                                  :placeholder current-nick}]
       [:button.btn.btn-flat {:on-click #(do (rf/dispatch [:conn/send [:chat/set-nick @new-nick]])
                                             (reset! new-nick ""))} "Set nick"]])))

(defview :participants
  (fn [nicks]
    [:ul.collection.with-header.participants.z-depth-1
     [view :set-nick]
     (for [nick nicks]
       [:li.collection-item {:key nick
                             :style {:color "#26a69a"}}
        [:span.activity-indicator.badge.white-text.teal "on"]
        [:div.nick nick]])]))

(defn send-box []
  (r/with-let [input-msg (r/atom "")
               send-fn (fn [text]
                         (rf/dispatch [:conn/send [:chat/add-message text]])
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

(defview :messages
  (fn [{:keys [is-empty messages]}]
    [:div.messages
     (if is-empty
       [:div.empty "No talking yet, be the first!"]
       [:ul
        (doall
         (for [{:keys [id nick is-my text received-at]} messages]
           [:li.collection-item {:key id
                                 :class (when is-my "my")}
            [:div.from nick]
            [:div.received-at received-at]
            [:div.text text]]))]
       )]))

(defn chat []
  [:div.chat.card.z-depth-1 {:class (when (<sub [:db/get-in [:is-chat-open]]) "open")}
   [send-box]
   [view :messages]
   [view :participants]
   ])
