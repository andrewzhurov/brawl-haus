(ns brawl-haus.receiver
  (:require [re-frame.core :as rf]
            [brawl-haus.utils :refer [<sub l]]))

(rf/reg-event-db
 :current-public-state
 (fn [db [_ public-state]]
   (assoc db :public-state public-state)))
