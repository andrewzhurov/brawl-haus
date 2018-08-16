(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.conn :as conn]
            [brawl-haus.state :as state]
            [brawl-haus.data :as data]
            [brawl-haus.events :as events]
            )
  (:gen-class))

(defn l [desc expr] (println desc expr) expr)

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" req
       (with-channel req channel              ; get the channel
         ;; communicate with client using method defined above
         (let [conn-id (conn/new-connection channel)]
           (swap! state/public-state events/drive [:conn/on-create conn-id])

           (on-close channel (fn [status]
                               (println "channel closed")))
           (on-receive channel (fn [data]
                                 (swap! state/public-state events/drive (read-string data) conn-id))))))
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
  (reset! state/public-state state/init-public-state)
  (restart-server))

(defn -main [& args]
  (reload))
