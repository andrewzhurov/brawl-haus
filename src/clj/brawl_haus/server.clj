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

(def <sub (comp deref rf/subscribe))
(defn <=evt [conn-id evt]
  (send! (:conn (<sub [:user nil conn-id])) (pr-str evt)))


(defn l [desc expr] (println desc expr) expr)
(defn uuid [] (str (java.util.UUID/randomUUID)))

(def init-db {:open-races {}
              :messages #{}
              :subs #{}})
(rf/reg-event-db
 :init-db
 (fn [_ _] init-db))
(rf/dispatch-sync [:init-db])

(add-watch re-frame.db/app-db :propagate-derived-data
           (fn [key atom old-state new-state]
             (doseq [[sub-id {:keys [conn-id]} & params :as sub] (:subs new-state)]
               (try (<=evt conn-id [:derived-data (into [sub-id] params) (l (str "calc sub " sub ": ") @(rf/subscribe sub))])
                    (catch Exception e (l "!calc sub fail: " sub))))
             ))

(rf/reg-event-db
 :subscribe
 (fn [db [_ {:keys [conn-id]} [sub-id & params] :as all]]
   (l "new sub: " (pr-str all))
   (update db :subs (fn [old] (conj (set old) (into [sub-id {:conn-id conn-id}]
                                                    params))))))

(rf/reg-event-db
 :unsubscribe
 (fn [db [_ {:keys [conn-id]} [sub-id & params]]]
   (update db :subs disj (into [sub-id {:conn-id conn-id}]
                               params))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" req
       (with-channel req channel              ; get the channel
         ;; communicate with client using method defined above
         (let [conn-id (uuid)]
           (rf/dispatch-sync [:conn/on-create {:conn-id conn-id
                                               :conn channel}])

           (on-close channel (fn [status]
                               (rf/dispatch-sync [:conn/on-close {:conn-id conn-id}])
                               (println "Connection closed: " conn-id)))
           (on-receive channel (fn [data]
                                 (let [[evt-id & evt-params :as evt] (read-string data)]
                                   (when (not (vector? evt))
                                     (throw (Exception. (str "Expect event be a vector, but got: " (pr-str evt) " :/"))))
                                   (rf/dispatch-sync (l "dispatch: " (into [evt-id
                                                                            {:conn-id conn-id}]
                                                                           evt-params)))))))))
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
  (restart-server))

(defn inspect-db []
  (clojure.pprint/pprint @re-frame.db/app-db))

(defn -main [& args]
  (reload))


(rf/reg-cofx
 :now
 (fn [cofx _]
   (assoc cofx :now (java.util.Date.))))

(rf/reg-event-db
 :test2
 (fn [_ _]
   (println "BLAH")))

(rf/reg-event-fx
 :test1
 [(rf/inject-cofx :now nil)]
 (fn [cofx _]
   (println cofx)))
