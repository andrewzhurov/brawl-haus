(ns brawl-haus.state
  (:require [brawl-haus.view-data :as view-data]
            [brawl-haus.conn :as conn]))

(defn l [desc expr] (println desc expr) expr)

(def init-public-state {:open-races {}
                        :messages #{}})
(def public-state (atom init-public-state))
(add-watch public-state :propagate-view-data
           (fn [key atom old-state new-state]
             (doseq [conn-id (l "KEYS:" (keys (:users (l "New state:" new-state))))
                     [sub-id _] (l "SUBS:" (get-in new-state [:users conn-id :view-data-subs]))]
               (let [view-data ((get view-data/view-data-fns sub-id) new-state conn-id)]
                 (conn/dispatch conn-id [:view-data sub-id view-data])))))
