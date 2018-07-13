(ns brawl-haus.panels.home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [cljs-time.coerce :as c]
            ))

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

(rf/reg-sub
 :users
 (fn [db _]
   (get-in db [:public-state :users])))

(defn participants []
  (let [users (sort-by :nick (<sub [:users]))
        {active true
         inactive false} (group-by (comp boolean :tube) users)]
    [:ul.collection.with-header.participants.z-depth-1
     [:li.collection-header.z-depth-1
      [:h6 "Active users (" (count active) ")"]]
     (for [{:keys [nick tube]} active]
       [:li.collection-item {:key nick
                             :style {:color "#26a69a"}}
        [:span.activity-indicator.badge.white-text.teal "on"]
        [:div.nick nick]])
     [:li.collection-header.z-depth-1
      [:h6 "Inactive users (" (count inactive) ")"]]
     (for [{:keys [nick tube]} inactive]
       [:li.collection-item {:key nick}
        [:span.activity-indicator.badge.white-text "off"]
        [:div.nick nick]])]
     ))


(rf/reg-sub
 :messages
 (fn [db _]
   (reverse
    (sort-by :received-at (get-in db [:public-state :messages])))))

(defmethod panels/panel :home-panel
  [_ route-params]
  [:div.app
   [panels/navbar]
   [:div.content.chat
    [send-box]
    [:div.messages
     (if-let [messages (not-empty (<sub [:messages]))]
       [:ul
        (doall
         (for [{:keys [id text from received-at]} (<sub [:messages])]
           [:li.collection-item {:key id
                                 :class (when (= (:nick from) (<sub [:nick])) "my")}
            [:div.from (:nick from)]
            [:div.received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))]
            [:div.text text]]))]
       [:div.empty "No talking yet, be the first!"])]
    [participants]
    ]])
