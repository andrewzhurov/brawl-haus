(ns brawl-haus.fit.lifecycle
  (:require [brawl-haus.fit.engine :as engine]
            [brawl-haus.fit.state :as state]
            [brawl-haus.fit.events :refer [>evt]]
            [brawl-haus.fit.entities :as entities]))

(defn init []
  (state/init!)
  (engine/start-engine!)
  (>evt [:add-ent entities/floor])
  (doseq [stair entities/stairs]
    (>evt [:add-ent stair]))
  (>evt [:add-ent entities/the-player])
  (doseq [e entities/enemies]
    (>evt [:add-ent e])))

(defn destroy [] (engine/stop-engine!))
