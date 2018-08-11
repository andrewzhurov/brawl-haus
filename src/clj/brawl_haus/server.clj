(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.data :as data]
            [brawl-haus.events :as events])
  (:gen-class))

(defn l [desc expr] (println desc expr) expr)
(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))

(def connections (atom {}))
(add-watch connections :connections-sync
           (fn [key atom old-state new-state]
             (doseq [[conn-id ch] new-state]
               (send! ch (pr-str [:conn/did-create conn-id])))))

(defn new-connection [ch]
  (let [connection-id (uuid)]
    (swap! connections assoc connection-id ch)
    connection-id))
(defn dispatch [conn-id evt]
  (send! (get @connections conn-id) (pr-str evt)))

(def init-public-state {:open-races {}
                        :messages #{}})
(def public-state (atom init-public-state))
(add-watch public-state :clients-sync
           (fn [key atom old-state new-state]
             (doseq [conn-id (l "KEYS:" (keys (:users (l "New state:" new-state))))]
               (dispatch conn-id [:current-public-state new-state]))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" req
       (with-channel req channel              ; get the channel
         ;; communicate with client using method defined above
         (let [conn-id (new-connection channel)]
           (swap! public-state events/drive [:conn/on-create conn-id])

           (on-close channel (fn [status]
                               (println "channel closed")))
           (on-receive channel (fn [data]
                                 (swap! public-state events/drive (read-string data) conn-id))))))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

(defonce server (atom nil))
(defn restart-server []
  (when @server
    (@server))
  (println "Running server on port 9090")
  (reset! server (run-server #'dev-handler {:port 9090})))

(defn reload []
  (use 'brawl-haus.server :reload)
  (reset! public-state init-public-state)
  (restart-server))

(defn -main [& args]
  (reload))
