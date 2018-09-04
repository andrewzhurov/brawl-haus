(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.events :as events]
            [brawl-haus.subs :as subs]
            [brawl-haus.subs]
            [clojure.pprint]

            [re-frame.core :as rf]
            [re-frame.db]
            )
  (:gen-class))
(defn l [desc expr] (println desc expr) expr)

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def last-derived (atom {}))

(defn conn [db conn-id]
  (l "FOUND CONN:" (get-in db [:users conn-id :conn])))

(add-watch events/db :propagate-derived-data
           (fn [key atom old-state new-state]
             (doseq [[sub conn-id] (:subs new-state)]
               (try
                 (let [derived-data (subs/derive new-state sub conn-id)]
                   (swap! last-derived
                          update sub
                          (fn [last-derived]
                            (when (not= derived-data last-derived)
                              (send! (conn new-state conn-id) (l "<=evt " (pr-str [:derived-data sub derived-data]))))
                            derived-data)))
                 (catch Exception e (println "!sub derive fail: " sub ". Message: " (.getMessage e) ". " e))))))



(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" req
       (with-channel req channel ; get the channel
         (let [conn-id (uuid)]
           (swap! events/db
                  events/drive [:conn/on-create {:conn-id conn-id
                                                 :conn channel}])

           (on-close channel (fn [status]
                               (swap! events/db
                                      events/drive [:conn/on-close conn-id])
                               (println "Connection closed: " conn-id)))
           (on-receive channel (fn [data]
                                 (swap! events/db
                                        events/drive (read-string data) conn-id))))))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

(defonce server (atom nil))
(defn restart-server []
  (when @server
    (@server))
  (println "Running server on port 9090")
  (reset! server (run-server #'dev-handler {:port 9090})))

(defn reload []
  (use 'brawl-haus.server :reload-all)
  (rf/dispatch-sync [:init-db])
  (restart-server))

(defn inspect-db []
  (clojure.pprint/pprint @events/db))

(defn -main [& args]
  (reload))
