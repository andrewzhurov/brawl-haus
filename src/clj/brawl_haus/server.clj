(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [pneumatic-tubes.core :refer [receiver transmitter dispatch]]
            [pneumatic-tubes.httpkit :refer [websocket-handler]]
            [brawl-haus.handler :as handler]

            [config.core :refer [env]]

            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.texts :as texts]
            [brawl-haus.my-tubes :as my-tubes])
  (:gen-class))

(defn l [desc expr] (println desc expr) expr)

(def tx (transmitter))          ;; responsible for transmitting messages to one or more clients
(def dispatch-to (partial dispatch tx)) ;; helper function to dispatch using this transmitter 

(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))

(def init-public-state {:open-races {}
                        :messages #{}})
(def public-state (atom init-public-state))
(add-watch public-state :clients-sync
           (fn [key atom old-state new-state]
             (dispatch-to (fn [x] true) [:current-public-state new-state])))

(defn user-publics [user] (select-keys user [:nick :user-since :tube]))
(def users (atom {}))
(add-watch users :user-data-client-sync
           (fn [key atom old-state new-state]
             (l "Old users state:" old-state)
             (l "New users state:" new-state)
             (swap! public-state assoc :users (map user-publics (vals new-state)))
             (let [altered-users (clojure.set/difference (set new-state) (set old-state))]
               (doall
                (map
                 (fn [[_ {:keys [tube] :as user}]]
                   (when tube
                     (dispatch-to  tube [:current-private-state {:user user}])))
                 altered-users)))))

(defn create-user [users nick pass]
  (assoc users nick {:nick nick
                     :pass pass
                     :user-since (now)
                     :tube nil}))
(defn auth-tube [users nick tube]
  (update-in users [nick :tube]
             (fn [old-tube]
               (l "OLD TUBE:" old-tube)
               (l "NEW TUBE:" tube)
               (when old-tube (dispatch-to old-tube [:outdated-connection]))
               tube)))

(defn auth [users nick pass tube]
  (let [user (get users nick)]
    (cond
      (and user (= pass (:pass user)))
      (do (dispatch-to tube [:success-login])
          (auth-tube users nick tube))

      user
      (do (dispatch-to tube [:failed-login])
          users)

      :newcomer
      (do (dispatch-to tube [:success-newcome])
          (-> users
              (create-user nick pass)
              (auth-tube nick tube))))))

(defn tube->user [tube]
  (l "User:" (first (filter (fn [user] (= (l "A TUBE:"(:tube user)) (l "The tube:" tube))) (vals @users)))))



(defn ?user [tube]
  (some-> tube
          tube->user
          user-publics))

;; {race-id : {nick: left%}}
(defn race [race-id]
  (get-in @public-state [:open-races race-id]))

#_(def race-status (atom {}))
#_(add-watch race-status :sync-race-status
           (fn [key atom old-state new-state]
             (let [altered-races (l "Diff:" (clojure.set/difference (l "New users:" (set new-state)) (l "Old users:" (set old-state))))
                   users @users]
               (doseq [[race-id statuses] altered-races
                       [nick _] statuses]
                 (let [tube (l "A tube of altered race:"(get-in users [nick :tube]))]
                   (dispatch-to tube [:db/set-in [:race-status race-id] statuses]))))))

(defn start-race [race]
  (-> race
      (assoc :status :countdown)
      (assoc :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date)))
      (assoc :race-text (rand-nth texts/longs))))

(def rx                         ;; collection of handlers for processing incoming messages
  (receiver
   {:login
    (fn [tube [_ {:keys [nick pass]}]]
      (l "Tube on login:" tube)
      (swap! users auth nick pass tube)
      tube)

    :sync-public-state
    (fn [tube _]
      (dispatch-to tube [:current-public-state @public-state])
      tube)

    :init
    (fn [tube _]
      (dispatch-to tube [:current-public-state @public-state])
      (dispatch-to tube [:init tube])
      tube)

    :add-message
    (fn [tube [_ text]]
      (when-let [user (?user tube)]
        (swap! public-state update :messages conj {:text text
                                                   :from user
                                                   :id (uuid)
                                                   :received-at (now)}))
      tube)

    :new-race
    (fn [tube _]
      (when-let [user (tube->user tube)]
        (l "Tube on new race:" tube)
        (l "User on new race:" user)
        (let [new-race {:id (uuid)
                        :initiated-at (now)
                        :initiator tube
                        :participants {(:nick user) nil}
                        :status :not-started}]
          (swap! public-state update :open-races assoc (:id new-race) new-race)
          (dispatch-to tube [:race-initiated new-race])
          ))
      tube)

    :enter-race
    (fn [tube [_ race-id]]
      (l "Tube on enter:" tube)
      (when-let [user (tube->user tube)]
        (l "User on enter:" user)
        (swap! public-state
               #(update-in % [:open-races race-id]
                           (fn [race]
                             (if (and (= :not-started (:status race))
                                      (not (contains? (:participants race) (:nick user))))
                               (-> race
                                   (update :participants assoc (:nick user) nil)
                                   start-race)
                               race)))
               ))
      tube)

    :left-text
    (fn [tube [_ race-id left-text]]
      (when-let [{:keys [nick]} (?user tube)]
        (swap! public-state update-in [:open-races race-id ]
               (fn [race]
                 (-> race
                     (assoc :status :ongoing)
                     (assoc-in [:participants nick] (count left-text))
                     (assoc-in [:finished nick] (when (zero? (count left-text)) (now)))))))
      tube)

    }))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" [] (my-tubes/websocket-handler rx {:on-tube-close (fn [tube]
                                                                   (l "On close of tube:" tube)
                                                                   (swap! users assoc-in
                                                                          [(:nick (l "Of user:" (tube->user tube))) :tube] nil))}))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

#_(def handler (fn [req] (clojure.pprint/pprint req) ((websocket-handler rx) req)))
    ;; kttp-kit based WebSocket request handler
    ;; it also works with Compojure routes

(defonce server (atom nil))

(defn restart-server []
  (when @server
    (@server))
  (reset! server (l "SERVER:" (run-server #'dev-handler {:port 9090}))))

(defn -main [& args]
  (println "IN MAIN")
  (restart-server))

(defn reload []
  (use 'brawl-haus.server :reload)
  (reset! public-state init-public-state)
  (restart-server))
