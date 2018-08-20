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
      (let [send-fn #(do (rf/dispatch [:conn/send [:chat/set-nick @new-nick]])
                         (reset! new-nick ""))]
        [:div.set-nick
         [:input.collection-header {:value @new-nick
                                    :on-change #(reset! new-nick (l "VAL:" (.-value (.-target %))))
                                    :on-key-down #(when (= (.-keyCode %) 13)
                                                    (send-fn))
                                    :placeholder current-nick}]
         [:button.btn.btn-flat {:on-click send-fn} "Set nick"]]))))

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

(rf/reg-event-fx
 :chat/display
 (fn [{:keys [db]} [_ evt]]
   (l "EVT:" evt)
   (case (l "OP:L"(:is-chat-open db))
     true (case evt
            :hide {:dispatch [:chat/hide]}
            :open {}
            :toggle {:dispatch [:chat/hide]})
     false (case evt
             :hide {}
             :open {:dispatch [:chat/show]}
             :toggle {:dispatch [:chat/show]}))))

(rf/reg-event-db
 :chat/show
 (fn [db _]
   (.. js/document (getElementById "chat-input") focus)
   (-> db
       (assoc :is-chat-open true)
       (assoc :is-help-open false))))
(rf/reg-event-db
 :chat/hide
 (fn [db _]
   (focus/focus-previous)
   (-> db
       (assoc :is-chat-open false)
       (assoc :is-help-open false))))

(defn chat []
  [:div.chat.card.z-depth-1 {:class (when (<sub [:db/get-in [:is-chat-open]]) "open")}
   [send-box]
   [view :messages]
   [view :participants]
   ])
