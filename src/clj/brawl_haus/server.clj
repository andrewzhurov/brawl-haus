(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [pneumatic-tubes.core :refer [receiver transmitter dispatch]]
            [pneumatic-tubes.httpkit :refer [websocket-handler]]

            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]

            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [brawl-haus.data :as data])
  (:gen-class))

(defn l [desc expr] (println desc expr) expr)
(defn uuid [] (str (java.util.UUID/randomUUID)))
(defn now [] (java.util.Date.))
(defn id [tube] (:tube/id tube))

(def tx (transmitter)) ;; responsible for transmitting messages to one or more clients
(def dispatch-to (partial dispatch tx)) ;; helper function to dispatch using this transmitter


(def init-public-state {:open-races {}
                        :messages #{}})
(def public-state (atom init-public-state))
(add-watch public-state :clients-sync
           (fn [key atom old-state new-state]
             (dispatch-to (fn [x] true) [:current-public-state new-state])))

(defn tube->user [tube]
  (get-in @public-state [:users tube]))

(defn race [race-id]
  (get-in @public-state [:open-races race-id]))


;; Hardcoded to start in 10 sec
(defn schedule-race [race]
  (assoc race :starts-at (-> (t/now) (t/plus (t/seconds 10)) (c/to-date))))

(defn ready-set-go [{:keys [id] :as race}]
  (future (Thread/sleep 7000)
          (swap! public-state update-in [:open-races id]
                 merge {:status :began
                        :race-text (rand-nth data/long-texts)}))
  race)


(defn calc-speed [text start finish]
  (let [minutes (/ (t/in-seconds (t/interval (c/from-date start)
                                             (c/from-date finish)))
                   60)
        chars (count text)]
    (int (/ chars minutes))))

(defn race-to-be [state]
  (let [[[_ race]] (filter (fn [[_ {:keys [status]}]] (= :to-be status))
                           (get state :open-races))]
    race))


(defn ensure-race [state]
  (if-not (race-to-be state)
    (let [new-race (-> {:id (uuid)
                        :participants {}
                        :status :to-be}
                       schedule-race
                       ready-set-go)]
      (assoc-in state [:open-races (:id new-race)] new-race))
    state))

(defn navigate [state tube-id location]
  (assoc-in state [:users tube-id :location] location))

(defn enter-race [state tube-id]
  (let [race (race-to-be state)]
    (-> state
        (assoc-in [:open-races (:id race) :participants tube-id] {})
        (navigate tube-id {:location-id :race-panel
                           :params {:race-id (:id race)}}))))

(def rx ;; collection of handlers for processing incoming events
  (receiver
   {:tube/on-create
    (fn [tube _]
      (let [anonymous-user {:nick (rand-nth data/names)
                            :tube (id tube)}]
        (swap! public-state
               assoc-in [:users (id tube)] anonymous-user))
      (dispatch-to tube [:tube/did-create (id tube)]))

    :tube/on-destroy
    (fn [tube _]
      (swap! public-state
             navigate (id tube) {:location-id :quit}))

    :add-message
    (fn [tube [_ text]]
      (when-let [user (l "USER?: "(tube->user (id tube)))]
        (swap! public-state update :messages conj {:text text
                                                   :from user
                                                   :id (uuid)
                                                   :received-at (now)}))
      tube)

    :race/attend
    (fn [tube _]
      (swap! public-state #(-> %
                               ensure-race
                               (enter-race (id tube))))
      tube)

    :left-text
    (fn [tube [_ race-id left-text]]
      (when (tube->user (id tube))
        (let [is-finished (zero? (count left-text))
              speed (when is-finished
                      (let [{:keys [race-text starts-at]} (get-in @public-state [:open-races race-id])]
                        (calc-speed race-text starts-at (now))))]
          (swap! public-state assoc-in
                 [:open-races race-id :participants (id tube)]
                 {:left-chars (count left-text)
                  :speed speed})))
      tube)

    :chat/set-nick
    (fn [tube [_ nick]]
      (when (tube->user (id tube))
        (swap! public-state assoc-in
               [:users (id tube) :nick] nick))
      tube)

    }))


(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/tube" [] (websocket-handler rx))
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
