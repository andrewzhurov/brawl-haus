(ns brawl-haus.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]))

#_(def dev-handler (-> #'routes wrap-reload))

#_(def handler routes)
