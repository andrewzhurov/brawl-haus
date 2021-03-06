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
  (get-in db [:users conn-id :conn]))

(defn propagate-derived-data []
  (add-watch events/db :propagate-derived-data
             (fn [key atom old-state new-state]
               (when (not= old-state new-state)
                 (doseq [[sub conn-id] (:subs new-state)]
                   (try
                     (let [derived-data (#'subs/derive new-state sub conn-id)]
                       (swap! last-derived
                              update [sub conn-id]
                              (fn [last-derived]
                                (when (not= derived-data last-derived)
                                  (when-let [conn (conn new-state conn-id)]
                                    (send! conn (l "<=evt " (pr-str [:derived-data sub derived-data])))))
                                derived-data)))
                     (catch Exception e (println "!sub derive fail: " sub ". Message: " (.getMessage e) ". " e))))))))



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

(declare looper)
(defn reload []
  (when @server
    (@server))
  (remove-watch events/db :propagate-derived-data)

  (when (future? looper) (future-cancel looper))
  (def looper (future
                (loop [now (t/now)]
                  (swap! events/db assoc :now (t/now))
                  (Thread/sleep 300)
                  (recur (t/now)))))

  (require 'brawl-haus.subs :reload-all)
  (require 'brawl-haus.events :reload-all)
  (use 'brawl-haus.server :reload-all)
  (reset! events/db events/init-db)
  (propagate-derived-data)
  (reset! server (run-server #'routes {:port 9090}))
  (println "Running server on port 9090"))

(defn inspect-db []
  (clojure.pprint/pprint @events/db))

(defn -main [& args]
  (reload))

#_(events/add-evt
 :reset
 (fn [_ _ conn-id]
   (println "RESET BACKEND")
   (reload)))

(+ 1 1) 
