(ns brawl-haus.ws
  (:require [re-frame.core :as rf :refer [reg-event-db reg-sub dispatch after]]
            [pneumatic-tubes.core :as tubes]
            [brawl-haus.utils :refer [<sub l]]
            [config.core :refer [env]]))

(reg-event-db
 :conn/did-create
 (fn [db [_ conn-id]]
   (assoc db :conn-id conn-id)))

(rf/reg-event-db
 :conn/create
 (fn [db _]
   (let [url (str "ws://" (:server-url env) "/tube")
         ws (js/WebSocket. url)]
     (set! (.-onopen ws) (fn [_] (l "WS opened for:" url)))
     (set! (.-onmessage ws) (fn [msg] (rf/dispatch (l "<=evt" (cljs.reader/read-string (.-data msg))))))
     (assoc db :ws ws))))

(rf/reg-event-fx
 :conn/send
 (fn [{:keys [db]} [_ evt]]
   (.send (:ws db) (l "evt=>" (pr-str evt)))))

