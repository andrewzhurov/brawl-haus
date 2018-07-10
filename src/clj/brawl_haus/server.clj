(ns brawl-haus.server
  (:use org.httpkit.server)
  (:require [pneumatic-tubes.core :refer [receiver transmitter dispatch]]
            [pneumatic-tubes.httpkit :refer [websocket-handler]]
            #_[brawl-haus.handler :refer [handler]]

            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))


(def tx (transmitter))          ;; responsible for transmitting messages to one or more clients
(def dispatch-to (partial dispatch tx)) ;; helper function to dispatch using this transmitter 

(def rx                         ;; collection of handlers for processing incoming messages
  (receiver
    {:say-hello                 ;; re-frame event name
     (fn [tube [_ name]]        ;; re-frame event handler function
       (println "Hello" name)
       (dispatch-to (fn [x] (println "A tube:" x) true) #_tube [:say-hello-processe])  ;; send event to same 'tube' where :say-hello came from
       #_from)
     :add-message
     (fn [tube [_ msg]]
       (dispatch-to (fn [x] true) [:added-message msg]))}))

(def handler (websocket-handler rx))   ;; kttp-kit based WebSocket request handler
                                       ;; it also works with Compojure routes

(defonce server (atom nil))

(defn l [desc expr] (println desc expr) expr)

(defn restart-server []
  (when @server
    (@server))
  (reset! server (l "SERVER:" (run-server handler {:port 9090}))))

(defn -main [& args]
  (println "IN MAIN")
  (restart-server))


