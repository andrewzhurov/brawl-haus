(ns brawl-haus.ws
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [brawl-haus.utils :refer [<sub l]]
            [config.core :refer [env]]
            [cljs.core.async :refer [close! chan <! put!]]))

(defn- start-send-loop! [socket out-queue]
  (go-loop
      [evt (<! out-queue)]
    (when (and evt (= (.-readyState socket) 1))
      (.send socket (pr-str evt))
      (l "=>evt" evt)
      (recur (<! out-queue)))))

(reg-event-db
 :conn/did-create
 (fn [db [_ conn-id]]
   (assoc db :conn-id conn-id)))

(rf/reg-event-db
 :conn/create
 (fn [db _]
   (let [url (str "ws://" (:server-url env) "/tube")
         ws (js/WebSocket. url)
         out-queue (chan)]
     (set! (.-onopen ws) (fn [_]
                           (l "WS opened for:" url)
                           (start-send-loop! ws out-queue)
                           ))
     (set! (.-onmessage ws) (fn [msg] (rf/dispatch (l "<=evt" (cljs.reader/read-string (.-data msg))))))
     (assoc db :out-queue out-queue))))

(rf/reg-event-fx
 :conn/send
 (fn [{:keys [db]} [_ evt]]
   (let [ch (:out-queue db)]
     (if ch
       (put! ch evt)
       (throw (js/Error. "Out queue does not exist"))))
   {}))

