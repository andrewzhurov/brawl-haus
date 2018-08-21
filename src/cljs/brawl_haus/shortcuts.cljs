(ns brawl-haus.shortcuts
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]
            [brawl-haus.utils :refer [l <sub >evt]]))

;; Exceptionally useful: https://unixpapa.com/js/testkey.html
;; If hungry on knowledge: https://unixpapa.com/js/key.html

(def shortcuts
  [{:name "Hide controls"
    :human-combo "Release Ctrl"
    :evt [:help/hide]
    :key-seq {:which 17
              :ctrlKey false}}
   {:name "Toggle chat"
    :human-combo "Ctrl + c"
    :evt [:chat/display :toggle]
    :key-seq {:which 67
              :ctrlKey true}}
   {:name "Next race"
    :human-combo "Ctrl + ->"
    :evt [:conn/send [:race/attend]]
    :key-seq {:which 39
              :ctrlKey true}}
   {:name "Go home"
    :human-combo "Ctrl + <-"
    :evt [:conn/send [:home/attend]]
    :key-seq {:which 37
              :ctrlKey true}}])

(rf/reg-event-db
 :help/show
 (fn [db _] (assoc db :is-help-shown true)))
(rf/reg-event-db
 :help/hide
 (fn [db _] (assoc db :is-help-shown false)))
(rf/reg-sub
 :help
 (fn [db _] (:is-help-shown db)))


(defn help []
  [:div.help.collection.with-header.card.z-depth-2
   {:class (when (<sub [:help]) "open")}
   [:div.collection-header [:h5.header "Controls"] [:h7.description "(click to fire)"]]
   (for [{:keys [name human-combo evt]} shortcuts]
     [:a.collection-item.waves-effect {:key name
                                       :on-click #(rf/dispatch evt)}
      [:div name [:div.secondary-content human-combo]]])])

(defn e->data [e]
  {:which (.-which e)
   :ctrlKey (.-ctrlKey e)})

(defn handle-keydown [e]
  (let [data (l "keydown:" (e->data e))
        {:keys [evt key-seq]} {:evt [:help/show]
                               :key-seq {:which 17
                                         :ctrlKey true}}]
    (when (= key-seq data)
      (>evt evt))))

(defn handle-keyup [e]
  (let [data (l "keyup:" (e->data e))]
    (doseq [{:keys [evt key-seq]} shortcuts]
      (when (= key-seq data)
        (>evt evt)))))

(defn reg-press-handlers []
  (.addEventListener js/window "keydown" handle-keydown)
  (.addEventListener js/window "keyup" handle-keyup))
