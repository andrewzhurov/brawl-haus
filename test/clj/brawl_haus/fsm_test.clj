(ns brawl-haus.fsm-test
  (:require  [clojure.test :refer [deftest testing is]]
             [re-frame.interop]
             [re-frame.core :as rf]
             [re-frame.db]
             [brawl-haus.shared.events]
             [brawl-haus.shared.subs]
             [day8.re-frame.test :refer [run-test-sync run-test-async]]
             [matcho.core :refer [assert]]
             ))

(defn l [desc expr] (println desc expr) expr)
(def <sub (comp deref rf/subscribe))
(defn >evt [evt & [conn-id]]
  (when (not (vector? evt))
    (throw (Exception. (str "Expect event be a vector, but got: " (pr-str evt) " :/"))))
  (if-not conn-id
    (rf/dispatch (l ">evt " evt))
    (let [[evt-id & evt-params] evt]
      (rf/dispatch (l ">evt " (into [evt-id
                                     conn-id]
                                    evt-params))))))

(defn init-=>evt [conn-id]
  (fn [evt] (>evt evt conn-id)))
(defn init-<=sub [conn-id]
  (fn [[sub-id & params]] (<sub (into [sub-id conn-id] params))))

(deftest fsm-test
  (with-redefs [re-frame.db/app-db (re-frame.interop/ratom {})]
    (run-test-sync
     (rf/dispatch [:inspect-db])
     (let [conn-id1 "test-conn-id1"
           conn-id2 "test-conn-id2"
           =>evt1 (init-=>evt conn-id1)
           =>evt2 (init-=>evt conn-id2)
           <=sub1 (init-<=sub conn-id1)
           <=sub2 (init-<=sub conn-id2)]
       (=>evt1 [:sv/attend])
       (assert {:location-id :space-versus}
               (<=sub1 [:location]))
       (assert {:ship {conn-id1 {}}}
               (<=sub1 [:sv]))

       (=>evt2 [:sv/attend])

       (assert {:shields {:in-use 0}
                :engines {:in-use 0}
                :weapons {:in-use 0}}
               (<=sub2 [:sv/systems]))

       (assert 5
               (<=sub2 [:sv/left-power]))

       (=>evt1 [:sv.system/power-up {:system-id :shields}])
       (=>evt1 [:sv.system/power-up {:system-id :shields}])

       (assert {:shields {:in-use 2}}
               (<=sub1 [:sv/systems]))

       ;; Not enough power
       (=>evt1 [:sv.weapon/power-up {:stuff-id :burst-laser-2}])
       (assert {:is-on false}
               (<=sub1 [:sv.weapon/readiness {:stuff-id :burst-laser-2}]))

       ;; Not going through top
       (=>evt1 [:sv.system/power-up {:system-id :weapons}])
       (=>evt1 [:sv.system/power-up {:system-id :weapons}])
       (=>evt1 [:sv.system/power-up {:system-id :weapons}])
       (=>evt1 [:sv.system/power-up {:system-id :weapons}])
       (assert {:weapons {:in-use 3
                          :max 3}}
               (<=sub1 [:sv/systems]))

       ;; Enough power
       (=>evt1 [:sv.weapon/power-up {:stuff-id :burst-laser-2}])
       (assert {:is-on true
                :percentage number?
                :is-ready false}
               (<=sub1 [:sv.weapon/readiness {:stuff-id :burst-laser-2}]))

       ;; Not ready and did not fire
       (=>evt1 [:sv.weapon/hit {:ship-id conn-id2
                                :system-id :weapons
                                :stuff-id :burst-laser-2}])
       (assert {:weapons {:damaged 0}}
               (<=sub2 [:sv/systems]))

       (Thread/sleep 200)
       ;; Ready
       (assert {:is-on true
                :percentage 100
                :is-ready true}
               (<=sub1 [:sv.weapon/readiness {:stuff-id :burst-laser-2}]))

       ;; Enemy powers weapon
       (=>evt2 [:sv.system/power-up {:system-id :weapons}])
       (=>evt2 [:sv.system/power-up {:system-id :weapons}])
       (=>evt2 [:sv.weapon/power-up {:stuff-id :burst-laser-2}])
       (=>evt2 [:inspect-db])
       (assert {:is-on true}
               (<=sub2 [:sv.weapon/readiness {:stuff-id :burst-laser-2}]))

       ;; System gets damaged and weapon turns off
       (=>evt1 [:sv.weapon/hit {:ship-id conn-id2
                                :system-id :weapons
                                :stuff-id :burst-laser-2}])
       (assert {:weapons {:damaged 2
                          :in-use 1}}
               (<=sub2 [:sv/systems]))
       (assert {:is-on false}
               (<=sub2 [:sv.weapon/readiness]))
       ))))
