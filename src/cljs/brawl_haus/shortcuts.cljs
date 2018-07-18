(ns brawl-haus.shortcuts
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]
            [brawl-haus.utils :refer [l <sub]]))

(def shortcuts
  [{:name "Hide help"
    :human-combo "Ctrl + space"
    :evt [:help/fire]
    :key-seq { ;; ?
              :which 32
              :ctrlKey true}
    }
   {:name "Toggle chat"
    :human-combo "Ctrl + c"
    :evt [:help/fire [:toggle-chat]]
    :key-seq {:which 67 ;; c
              :ctrlKey true}
    }
   {:name "Next race"
    :human-combo "Ctrl + ->"
    :evt [:help/fire [:tube/send [:race/attend]]]
    :key-seq {:which 39 ;; ->
              :ctrlKey true}
    }
   ])

(rf/reg-event-fx
 :help/fire
 (fn [{:keys [db]} [_ evt]]
   (merge {:db (assoc db :is-help-open false)}
          (when evt {:dispatch evt}))))

(rf/reg-event-db
 :help/toggle
 (fn [db _] (update db :is-help-open not)))

(rf/reg-event-db
 :help/hide
 (fn [db _] (assoc db :is-help-open false)))

(rf/reg-event-db
 :help/open
 (fn [db _] (assoc db :is-help-open true)))

(rf/reg-sub
 :help
 (fn [db _] (:is-help-open db)))

(def keydown-rules
  [::rp/set-keydown-rules
   {:event-keys [[[:help/open]
                  [{:ctrlKey true}]]]

    :always-listen-keys
    [{:ctrlKey true}]
    :prevent-default
    [{:ctrlKey true}]
    }])

(def keyup-rules
  [::rp/set-keyup-rules
   {:event-keys (mapv (fn [{:keys [evt key-seq]}]
                        [evt
                         [key-seq]])
                      shortcuts)
    :always-listen-keys
    (mapv :key-seq shortcuts)
    }])


(defn help-btn []
  [:div.btn.help-btn {:on-click #(rf/dispatch [:help/toggle])}
   [:i.material-icons "help_outline"]])

(defn help []
  [:div.help.collection.with-header.card.z-depth-2
   {:class (when (<sub [:help]) "open")}
   [:div.collection-header [:h5.header "Controls"] [:h7.description "(click to fire)"]]
   (for [{:keys [name human-combo evt]} shortcuts]
     [:a.collection-item.waves-effect {:key name
                                       :on-click #(rf/dispatch evt)}
      [:div name [:div.secondary-content human-combo]]])])
