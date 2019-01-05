(ns brawl-haus.fit.mnemo
  (:require [brawl-haus.fit.state :as state]))

(def rewind-speed 2)
(defn mnemo-tick [db-now db-prev]
  (assoc db-now :prev-tick db-prev))

(defn back-tick [db]
  (if-let [pt (reduce (fn [db _]
                        (if-let [pt-inner (:prev-tick db)]
                          pt-inner
                          db))
                      db (range rewind-speed))]
    pt
    (do
      (println "LIMIT REACHED")
      db)))

(defn drop-controls [db]
  (assoc db :controls state/init-controls))

(def mnemo (atom {:current-reverse nil
                  :reverse-history #{}}))

(defn normal-time? [] (not (:current-reverse @mnemo)))

(defn timeline []
  (let [{:keys [evt-history current-tick]} @state/db
        f-domain 0
        l-domain current-tick
        axis-domain (- l-domain f-domain)
        axis-range 1180
        ->range (fn [domain-val] (* (/ axis-range axis-domain) (- domain-val f-domain)))]
    [:g
     (for [[tick evts] evt-history
           [idx [evt-id]] (partition 2 (interleave (range) evts))
           :when (not= evt-id :mouse)]
       (do #_(js/console.log "His item:" tick idx evt-id)
           ^{:key (str tick "-" idx)}
           [:circle {:cx (->range tick) :cy (* 10 (inc idx))
                     :r 3 :fill "rgba(255,255,0,0.6)"}]))
     (when (not (normal-time?))
       [:image {:xlinkHref "/sandbox/rewind.svg"
                :x 1060 :y 40
                :width 80
                }])
     ]))
