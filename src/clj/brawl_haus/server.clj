(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.shared.events]
            [brawl-haus.shared.subs]
            [clojure.pprint]

            [re-frame.core :as rf]
            [re-frame.db]
            )
  (:gen-class))
(defn l [desc expr] (println desc expr) expr)

(def <sub (comp deref rf/subscribe))
(defn >evt [evt & [conn-id]]
  (println "EVT FIRE" evt conn-id)
  (when (not (vector? evt))
    (throw (Exception. (str "Expect event be a vector, but got: " (pr-str evt) " :/"))))
  (if-not conn-id
    (rf/dispatch (l ">evt " evt))
    (let [[evt-id & evt-params] evt]
      (rf/dispatch (l ">evt " (into [evt-id
                                     conn-id]
                                    evt-params))))))

(defn <=evt [conn-id evt]
  (send! (:conn (<sub [:user nil conn-id])) (l "<=evt " (pr-str evt))))



(defn uuid [] (str (java.util.UUID/randomUUID)))

(def init-db {:open-races {}
              :messages #{}
              :subs #{}
              :games {:sv {}}})
(rf/reg-event-db
 :init-db
 (fn [_ _] init-db))
(rf/dispatch-sync [:init-db]) 

(def last-derived (atom {}))

(add-watch re-frame.db/app-db :propagate-derived-data
           (fn [key atom old-state new-state]
             (l "RAN" 11)
             (doseq [[sub-id conn-id & params :as sub] (:subs new-state)]
               (try
                 (let [derived-data (<sub sub)]
                   (swap! last-derived
                          update sub
                          (fn [last-derived]
                            (when (not= derived-data last-derived)
                              (<=evt conn-id [:derived-data (into [sub-id] params) derived-data]))
                            derived-data)))
                 (catch Exception e (println "!calc sub fail: " sub ". Message: " (.getMessage e) ". " e))))
             ))

(rf/reg-event-db
 :subscribe
 (fn [db [_ conn-id [sub-id & params] :as all]]
   (l "new sub: " (pr-str all))
   (update db :subs (fn [old] (conj (set old) (into [sub-id conn-id]
                                                    params))))))

(rf/reg-event-db
 :unsubscribe
 (fn [db [_ conn-id [sub-id & params]]]
   (update db :subs disj (into [sub-id conn-id]
                               params))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" req
       (with-channel req channel ; get the channel
         (let [conn-id (uuid)]
           (rf/dispatch-sync [:conn/on-create conn-id {:conn-id conn-id
                                                       :conn channel}])

           (on-close channel (fn [status]
                               (rf/dispatch-sync [:conn/on-close conn-id])
                               (println "Connection closed: " conn-id)))
           (on-receive channel (fn [data]
                                 (>evt (read-string data) conn-id))))))
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
  (clojure.pprint/pprint @re-frame.db/app-db))

(defn -main [& args]
  (reload))
