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
              :on-change #(reset! input-msg (.-value (.-target %)))
              :on-key-down #(when (= (.-keyCode %) 13)
                              (send-fn @input-msg))}
      ]
     [:div.btn.btn-flat {:class (when (empty? @input-msg) "disabled"):on-click #(send-fn @input-msg)}
      [:i.material-icons "send"]]]))


(rf/reg-sub
 :messages
 (fn [db _]
   (reverse
    (sort-by :received-at (get-in db [:public-state :messages])))))

(defmethod panels/panel :home-panel
  [_ route-params]
  [:div.app
   [panels/navbar]
   #_[:div.tube-indicator {:class (boolean @(rf/subscribe [:db/get-in [:tube]]))}]
   #_[:button {:on-click #(rf/dispatch [:tube/send [:sync-public-state]])} "Sync public state"]
   [:div.content.chat
    [send-box]
    [:div.messages
     (if-let [messages (not-empty (<sub [:messages]))]
       [:ul
        (doall
         (for [{:keys [id text from received-at]} (<sub [:messages])]
           [:li.collection-item {:key id
                                 :class (when (= (:nick from) (<sub [:nick])) "my")}
            #_(do (l "FROM:" from) (l "TUBE:" (<sub [:tube])) nil)
            [:div.from (:nick from)]
            [:div.received-at (f/unparse (f/formatter "HH:mm:ss") (c/from-date received-at))]
            [:div.text text]]))]
       [:div "No talking yet, be the first!"])]
    ]])
