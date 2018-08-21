(ns brawl-haus.conn
  (:use org.httpkit.server))

(defn l [desc expr] (println desc expr) expr)
(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))

(def init-connections {})
(def connections (atom init-connections))
(add-watch connections :connections-sync
           (fn [key atom old-state new-state]
             (doseq [[conn-id {:keys [ch last-sent-personalized personalized]}] new-state]
               )))

(defn calc-view-data [conn-id view-id view-data]
  (swap! connections
         (fn [conns]
           (if (not= (get-in conns [conn-id :view-data view-id]) view-data)
             (do (send! (get-in conns [conn-id :ch]) (pr-str [:view-data view-id view-data]))
                 (assoc-in conns [conn-id :view-data view-id] view-data))
             conns))))

(defn new-connection [ch]
  (let [connection-id (uuid)]
    (send! ch (pr-str [:conn/did-create connection-id]))
    (swap! connections assoc connection-id {:ch ch
                                            :view-data {}})
    connection-id))

(defn dispatch [conn-id evt]
  (send! (get-in @connections [conn-id :ch]) (pr-str evt)))
