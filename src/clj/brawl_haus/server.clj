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

            [brawl-haus.texts :as texts])
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

(def users (atom {}))
(add-watch users :user-data-client-sync
           (fn [key atom old-state new-state]
             (let [altered-users (l "Diff:" (clojure.set/difference (l "New users:" (set new-state)) (l "Old users:" (set old-state))))]
               (doall
                (map
                 (fn [[_ {:keys [tube] :as user}]]
                   (dispatch-to (l "To tube:" tube) (l "Evt:" [:current-private-state {:user user}])))
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

(defn user-publics [user]
  (l "Publics:" (select-keys user [:id :nick :user-since])))

(defn ?user [tube]
  (some-> tube
          tube->user
          user-publics))

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
      (l "TUBE1111:" tube)
      (let [new-race {:id (uuid)
                      :initiated-at (now)
                      :initiator tube
                      :participants #{tube}
                      :status :not-started}]
        (swap! public-state update :open-races assoc (:id new-race) new-race)
        (java.lang.Thread/sleep 3000)
        (dispatch-to tube [:race-initiated new-race])
        )
      tube)

    :enter-race
    (fn [tube [_ race-id]]
      (l "Tube on enter:" tube)
      (swap! public-state
             #(update-in % [:open-races race-id]
                         (fn [race]
                           (if (= :not-started (:status race))
                             (-> race
                                 (update :participants conj tube)
                                 (assoc :status :countdown)
                                 (assoc :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date)))
                                 (assoc :race-text (rand-nth texts/longs)))
                             race)))
             )
      tube)
    }))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" [] (websocket-handler rx))
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
