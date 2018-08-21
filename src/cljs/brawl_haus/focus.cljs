(ns brawl-haus.focus
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.utils :refer [l <sub]]))

(rf/reg-event-db
 :focus
 (fn [db [_ target-id]]
   (when-let [target (js/document.getElementById target-id)]
     (.focus target))
   (update db :focus-state (fn [{:keys [current-node] :as all}]
                             (if (not= current-node target-id)
                               {:current-node target-id
                                :previous-node current-node}
                               all)))))

(rf/reg-event-fx
 :focus/previous
 (fn [{:keys [db]} _]
   (when-let [target-id (get-in db [:focus-state :previous-node])]
     {:dispatch [:focus target-id]})))
